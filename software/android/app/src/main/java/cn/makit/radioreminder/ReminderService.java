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
 final MissedEngine engine=new MissedEngine();
 final IncomingEpisode episode=new IncomingEpisode();
 BleLink link;TelephonyManager phone;PhoneStateListener phoneListener;
 ContentObserver observer;BroadcastReceiver clockReceiver;
 long startedWall,connectedWall,alertAt;boolean stopping,demoMode,wasReady;
 int lastState=TelephonyManager.CALL_STATE_IDLE;long lastSync;
 final Set<String> processedLogs=new HashSet<>();final ArrayDeque<String> processedOrder=new ArrayDeque<>();
 public final android.os.IBinder onBind(Intent i){return null;}
 boolean granted(String p){return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
 static void log(String s){event=s;logs.addFirst(new java.text.SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date())+" "+s);while(logs.size()>30)logs.removeLast();}
 public void onCreate(){super.onCreate();live=this;startedWall=System.currentTimeMillis();createChannel(this);link=new BleLink(this,this);}
 public int onStartCommand(Intent i,int flags,int id){
  startForeground(1,notice(this,"挂件服务正在运行；点此查看连接状态"));
  if(phoneListener==null)watchCalls();ensureObserver();
  if(i!=null&&"SCAN".equals(i.getAction()))link.scan();
  else if(i!=null&&"CONNECT".equals(i.getAction())){String mac=i.getStringExtra("address");if(mac!=null){getSharedPreferences("settings",0).edit().putString("address",mac).apply();link.connect(mac);}}
  else if(i!=null&&"STOP".equals(i.getAction())){stopping=true;link.send(Protocol.STOP,0);h.postDelayed(this::stopSelf,250);}
  else if(!link.ready()){String mac=getSharedPreferences("settings",0).getString("address",null);if(mac!=null)link.connect(mac);else connection="请先查找并选择挂件";}
  h.removeCallbacks(tick);h.post(tick);return START_NOT_STICKY;
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
  if(state==lastState)return;lastState=state;
  if(state==TelephonyManager.CALL_STATE_RINGING){episode.ring(System.currentTimeMillis(),SystemClock.elapsedRealtime());log("检测到来电，等待最终结果");}
  if(state==TelephonyManager.CALL_STATE_OFFHOOK){episode.clear();stopAlert("已接听或开始通话，清空漏接序列");}
  if(state==TelephonyManager.CALL_STATE_IDLE&&episode.idle(SystemClock.elapsedRealtime())){h.postDelayed(this::readCallLog,1500);h.postDelayed(this::notificationCandidate,1600);}
 }
 boolean compatible(){return getSharedPreferences("settings",0).getBoolean("compatible",false);}
 void readCallLog(){
  if(!granted(Manifest.permission.READ_CALL_LOG))return;
  try(Cursor c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls._ID,CallLog.Calls.DATE,CallLog.Calls.TYPE},CallLog.Calls.DATE+">=?",new String[]{Long.toString(Math.max(startedWall,connectedWall))},CallLog.Calls.DATE+" ASC")){
   if(c==null)return;
   while(c.moveToNext()){
    String id=c.getString(0);long date=c.getLong(1);int type=c.getInt(2);
    if(processedLogs.contains(id))continue;
    processedLogs.add(id);processedOrder.add(id);if(processedOrder.size()>256)processedLogs.remove(processedOrder.removeFirst());
    if(System.currentTimeMillis()-date>180000||date-System.currentTimeMillis()>5000)continue;
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
  String id=episode.consume(SystemClock.elapsedRealtime());if(id!=null)missed(id,true);
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
 public void onDestroy(){stopping=true;h.removeCallbacksAndMessages(null);if(phone!=null&&phoneListener!=null)try{phone.listen(phoneListener,PhoneStateListener.LISTEN_NONE);}catch(Exception ignored){}if(observer!=null)getContentResolver().unregisterContentObserver(observer);if(clockReceiver!=null)unregisterReceiver(clockReceiver);link.close();live=null;connection="服务已停止";super.onDestroy();}
}
