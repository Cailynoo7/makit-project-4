package cn.makit.radioreminder;
import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.CallLog;
import android.telephony.*;
import java.util.*;
@SuppressWarnings("deprecation")
public final class ReminderService extends Service implements BleLink.Listener {
 static ReminderService live;
 public static String connection="尚未启动",event="等待首次设置",deviceStatus="尚无设备状态";
 static final ArrayDeque<String> logs=new ArrayDeque<>();
 static final String CHANNEL="connection";
 final Handler h=new Handler(Looper.getMainLooper());
 final Runnable finishStop=()->{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();};
 final MissedEngine engine=new MissedEngine();
 final IncomingEpisode episode=new IncomingEpisode();
 CallHistoryDb historyDb;long historyId,notificationHistoryId;
 BleLink link;TelephonyManager phone;PhoneStateListener phoneListener;
 ContentObserver observer;BroadcastReceiver clockReceiver;
 long startedWall,connectedWall,alertAt,lastPhoneUpdate;boolean stopping,demoMode,wasReady,connectionRequested;
 int lastState=TelephonyManager.CALL_STATE_IDLE;long lastSync;
 final Set<String> processedLogs=new HashSet<>();final ArrayDeque<String> processedOrder=new ArrayDeque<>();
 public final android.os.IBinder onBind(Intent i){return null;}
 boolean granted(String p){return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
 static void log(String s){event=s;logs.addFirst(new java.text.SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date())+" "+s);while(logs.size()>30)logs.removeLast();}
 public void onCreate(){super.onCreate();live=this;startedWall=System.currentTimeMillis();historyDb=new CallHistoryDb(this);historyDb.recoverInterrupted();createChannel(this);link=new BleLink(this,this);}
 public int onStartCommand(Intent i,int flags,int id){
  String action=i==null?"SYSTEM_RESTART":i.getAction();
  if("STOP".equals(action)){
   MonitoringRecovery.setEnabled(this,false);stopping=true;h.removeCallbacks(tick);link.send(Protocol.STOP,0);h.postDelayed(finishStop,250);return START_NOT_STICKY;
  }
  if(!MonitoringRecovery.enabled(this)||!MonitoringRecovery.permissions(this)){
   if(MonitoringRecovery.enabled(this))MonitoringRecovery.blocked(this,"权限不足，无法恢复监听；请打开App授权");
   stopping=true;stopSelf();return START_NOT_STICKY;
  }
  h.removeCallbacks(finishStop);stopping=false;
  try{startForeground(1,notice(this,"来电监听正在运行；点此查看记录与挂件状态"));}
  catch(RuntimeException e){MonitoringRecovery.blocked(this,"前台监听启动失败："+e.getClass().getSimpleName());stopping=true;stopSelf();return START_NOT_STICKY;}
  String why=i==null?"系统重建服务":i.getStringExtra("recovery_reason");if(why==null)why="用户开启监听";
  if(i!=null&&i.hasExtra("phone_state")&&RecoveryPolicy.freshPhoneEvent(i.getLongExtra("phone_at",0),SystemClock.elapsedRealtime()))onPhoneState(i.getIntExtra("phone_state",TelephonyManager.CALL_STATE_IDLE));
  if(phoneListener==null)watchCalls();ensureObserver();h.post(()->readCallLog(false));
  if("SCAN".equals(action))link.scan();
  else if("CONNECT".equals(action)){String mac=i.getStringExtra("address");if(mac!=null){getSharedPreferences("settings",0).edit().putString("address",mac).apply();connectionRequested=true;link.connect(mac);}}
  else if(!link.ready()&&(!connectionRequested||!"RECOVER".equals(action))){String mac=getSharedPreferences("settings",0).getString("address",null);if(mac!=null){connectionRequested=true;link.connect(mac);}else connection="来电监听已开启；尚未选择挂件";}
  MonitoringRecovery.started(this,why);log("监听已启动："+why);
  h.removeCallbacks(tick);h.post(tick);return START_STICKY;
 }
 public void onTaskRemoved(Intent rootIntent){
  if(!stopping&&MonitoringRecovery.enabled(this))MonitoringRecovery.schedule(this,"最近任务已移除");
  super.onTaskRemoved(rootIntent);
 }
 static void createChannel(Context c){c.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"挂件连接与运行状态",NotificationManager.IMPORTANCE_LOW));}
 static Notification notice(Context c,String text){return new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("收音机提醒·验证版").setContentText(text).setOngoing(true).setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build();}
 private void watchCalls(){
  phone=getSystemService(TelephonyManager.class);
  phoneListener=new PhoneStateListener(){public void onCallStateChanged(int state,String ignored){onPhoneState(state);}};
  try{if(granted(Manifest.permission.READ_PHONE_STATE))phone.listen(phoneListener,PhoneStateListener.LISTEN_CALL_STATE);}catch(SecurityException e){log("电话状态权限不可用");}

  clockReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(Intent.ACTION_TIME_CHANGED.equals(i.getAction())){startedWall=System.currentTimeMillis();connectedWall=startedWall;stopAlert("手机时间变更，重新计数");}sync();if(MissedEngine.quiet(minute()))stopAlert("进入夜间静音");}};
  IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_TIME_CHANGED);f.addAction(Intent.ACTION_TIMEZONE_CHANGED);f.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);f.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(clockReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(clockReceiver,f);
 }
 void ensureObserver(){if(observer==null&&granted(Manifest.permission.READ_CALL_LOG)){try{observer=new ContentObserver(h){public void onChange(boolean self){readCallLog();}};getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI,true,observer);}catch(SecurityException e){observer=null;log("系统限制通话记录访问");}}}
 void onPhoneState(int state){
  lastPhoneUpdate=System.currentTimeMillis();
  if(state==lastState)return;lastState=state;
  if(state==TelephonyManager.CALL_STATE_RINGING){if(historyId>0)historyDb.result(historyId,CallHistoryState.UNKNOWN,"新来电到达，前一通结果未确认");historyId=historyDb.begin(System.currentTimeMillis());notificationHistoryId=historyId;episode.ring(System.currentTimeMillis(),SystemClock.elapsedRealtime());log("检测到来电，等待最终结果");}
  if(state==TelephonyManager.CALL_STATE_OFFHOOK){historyDb.result(historyId,CallHistoryState.ANSWERED,"手机来电状态：响铃后进入通话");notificationHistoryId=0;episode.clear();stopAlert("已接听或开始通话，清空漏接序列");}
  if(state==TelephonyManager.CALL_STATE_IDLE){historyDb.result(historyId,CallHistoryState.UNKNOWN,"电话已结束，等待系统核验");historyId=0;if(episode.idle(SystemClock.elapsedRealtime())){h.postDelayed(this::readCallLog,1500);h.postDelayed(this::notificationCandidate,1600);}}
 }
 boolean compatible(){return getSharedPreferences("settings",0).getBoolean("compatible",false);}
 void readCallLog(){readCallLog(true);}
 public void refreshHistory(){readCallLog(false);}
 void readCallLog(boolean allowAlert){
  if(!granted(Manifest.permission.READ_CALL_LOG))return;
  try(Cursor c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls._ID,CallLog.Calls.DATE,CallLog.Calls.TYPE},CallLog.Calls.DATE+">=?",new String[]{Long.toString(startedWall-CallObservation.MATCH_MS)},CallLog.Calls.DATE+" ASC")){
   if(c==null)return;
   while(c.moveToNext()){
    String id=c.getString(0);long date=c.getLong(1);int type=c.getInt(2);
    boolean liveSeen=false;
    if(type==CallLog.Calls.MISSED_TYPE)liveSeen=historyDb.system(id,date,CallHistoryState.MISSED);
    else if(type==CallLog.Calls.INCOMING_TYPE)liveSeen=historyDb.system(id,date,CallHistoryState.ANSWERED);
    else if(type==CallLog.Calls.REJECTED_TYPE)liveSeen=historyDb.system(id,date,CallHistoryState.REJECTED);
    // Importing a row must never manufacture proof of live monitoring or replay an alert.
    if(!allowAlert)continue;
    String processedKey=id+":"+type;
    if(processedLogs.contains(processedKey))continue;
    processedLogs.add(processedKey);processedOrder.add(processedKey);if(processedOrder.size()>256)processedLogs.remove(processedOrder.removeFirst());
    if(type==CallLog.Calls.OUTGOING_TYPE&&date>=startedWall&&date>=connectedWall&&System.currentTimeMillis()-date<=180000){stopAlert("系统确认拨出，清空序列");continue;}
    if(!CallObservation.canAlert(liveSeen,date,startedWall,connectedWall,System.currentTimeMillis()))continue;
    if(type==CallLog.Calls.MISSED_TYPE)missed("log:"+id,false);
    else if(type==CallLog.Calls.INCOMING_TYPE||type==CallLog.Calls.OUTGOING_TYPE)stopAlert("通话记录确认接听/拨出，清空序列");
    else if(type==CallLog.Calls.REJECTED_TYPE)log("主动拒接，不计入漏接");
   }
  }catch(SecurityException e){log("系统未允许读取通话记录，可使用通知兼容模式");}
 }
 void missedNotification(String key,long postTime){
  if(!compatible()||granted(Manifest.permission.READ_CALL_LOG))return;
  if(episode.notification(postTime,System.currentTimeMillis()))h.postDelayed(this::notificationCandidate,500);
 }
 void notificationCandidate(){
  if(!compatible()||granted(Manifest.permission.READ_CALL_LOG))return;
  String id=episode.consume(SystemClock.elapsedRealtime());if(id!=null){historyDb.result(notificationHistoryId,CallHistoryState.SUSPECTED,"系统未接通知与来电状态关联推断");notificationHistoryId=0;missed(id,true);}
 }
 int minute(){Calendar c=Calendar.getInstance();return c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);}
 Boolean silent(){try{int r=getSystemService(AudioManager.class).getRingerMode();int d=getSystemService(NotificationManager.class).getCurrentInterruptionFilter();if(r==AudioManager.RINGER_MODE_SILENT)return true;if(d!=NotificationManager.INTERRUPTION_FILTER_UNKNOWN&&d!=NotificationManager.INTERRUPTION_FILTER_ALL)return true;if(d==NotificationManager.INTERRUPTION_FILTER_UNKNOWN)return null;return false;}catch(Exception e){return null;}}
 void missed(String id,boolean inferred){
  if(demoMode&&!id.startsWith("demo:")){log("模拟测试中，真实事件暂不计数；清空计数后恢复");return;}
  long now=SystemClock.elapsedRealtime();boolean quiet=MissedEngine.quiet(minute());Boolean s=silent();
  if(engine.missed(id,now,s,quiet)){sync();link.send(Protocol.ALERT,0);alertAt=now;log((inferred?"通知推断：":"记录核验：")+"连续3通漏接，已发送提醒");}
  else log((inferred?"疑似漏接":"已核验漏接")+"；窗口计数 "+engine.count(now)+"/3"+(quiet?"（夜间无声）":Boolean.FALSE.equals(s)?"（手机非静音/勿扰，不触发）":""));
 }
 public void stopAlert(String why){demoMode=false;engine.reset();episode.clear();if(link!=null)link.send(Protocol.STOP,0);log(why);}
 public void sync(){if(link.ready()){link.send(Protocol.SYNC,minute());lastSync=SystemClock.elapsedRealtime();}}
 private final Runnable tick=new Runnable(){public void run(){if(stopping)return;long now=SystemClock.elapsedRealtime();if(link.ready()&&now-lastSync>=30000)sync();if(engine.active()&&(MissedEngine.quiet(minute())||now-alertAt>=120000))stopAlert("提醒结束或进入夜间，清空序列");h.postDelayed(this,5000);}};
 public void state(String s,boolean ready){connection=s;if(ready&&!wasReady){connectedWall=System.currentTimeMillis();episode.clear();}wasReady=ready;engine.connected(ready);if(!ready){episode.clear();}else{sync();}getSystemService(NotificationManager.class).notify(1,notice(this,s));}
 public void status(byte[] b){if(b.length!=10||b[0]!=1)return;int bits=Protocol.arg(b);getSharedPreferences("settings",0).edit().putInt("sound",(bits>>4)&1).putInt("volume",Math.min(2,(bits>>5)&3)).apply();deviceStatus="音色"+(((bits>>4)&1)+1)+" · 音量"+(((bits>>5)&3)+1)+((bits&1)!=0?" · 正在提醒":" · 待机")+((bits&2)==0?" · 时间未同步":"")+((bits&4)!=0?" · 夜间无声":"")+((bits&256)!=0?" · 低电量":"");if((b[1]&255)==0x81){demoMode=false;episode.clear();engine.reset();log("挂件单键已停止本轮提醒，清空漏接序列");}}
 public void found(BluetoothDevice d){Intent i=new Intent(MainActivity.FOUND).setPackage(getPackageName()).putExtra("address",d.getAddress());sendBroadcast(i);}
 public void simulate(){if(!link.ready()){log("请先连接挂件");return;}if(!demoMode||engine.active())stopAlert("开始独立模拟，清空计数可恢复真实监听");demoMode=true;missed("demo:"+System.nanoTime(),false);}
 public void preview(){sync();link.send(Protocol.PREVIEW,0);log(MissedEngine.quiet(minute())?"夜间静音，试听不发声":"已发送单次试听");}
 public void onDestroy(){boolean requestRecovery=!stopping&&MonitoringRecovery.enabled(this);stopping=true;h.removeCallbacksAndMessages(null);if(phone!=null&&phoneListener!=null)try{phone.listen(phoneListener,PhoneStateListener.LISTEN_NONE);}catch(Exception ignored){}if(observer!=null)getContentResolver().unregisterContentObserver(observer);if(clockReceiver!=null)unregisterReceiver(clockReceiver);historyDb.result(historyId,CallHistoryState.UNKNOWN,"监听已停止，最终结果未确认");historyDb.close();link.close();live=null;connection="服务已停止";if(requestRecovery)MonitoringRecovery.schedule(this,"监听服务已结束");super.onDestroy();}
}
