package cn.makit.radioreminder;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;
/** Best-effort OS-supported recovery. No exact alarms, wake locks or recurring polling. */
public final class MonitoringRecovery {
 static final String ALARM="cn.makit.radioreminder.RECOVERY_CHECK";
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("monitoring",0);}
 public static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false);}
 public static void setEnabled(Context c,boolean on){prefs(c).edit().putBoolean("enabled",on).commit();if(!on)cancel(c);}
 public static boolean permissions(Context c){return c.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED&&(Build.VERSION.SDK_INT<31||c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED);}
 public static void record(Context c,String message){prefs(c).edit().putString("last_recovery",new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date())+" "+message).apply();}
 public static String summary(Context c){SharedPreferences p=prefs(c);return p.getString("last_recovery","尚无恢复记录")+"\n最近自动启动："+p.getString("last_automatic_start","尚无")+"\n最近恢复失败："+p.getString("last_blocked","尚无");}
 static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,41,new Intent(c,RecoveryReceiver.class).setAction(ALARM),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 public static void cancel(Context c){AlarmManager a=c.getSystemService(AlarmManager.class);if(a!=null)a.cancel(alarm(c));}
 public static void schedule(Context c,String why){
  if(!enabled(c))return;
  AlarmManager a=c.getSystemService(AlarmManager.class);if(a==null)return;
  try{a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+5000,alarm(c));record(c,why+"；已请求一次恢复检查，系统可能延后");}
  catch(RuntimeException e){blocked(c,"无法安排恢复检查："+e.getClass().getSimpleName());}
 }
 public static boolean start(Context c,String why,int phoneState,long phoneAt){
  if(!enabled(c))return false;
  if(!RecoveryPolicy.mayRecover(enabled(c),permissions(c))){blocked(c,"恢复失败：电话或蓝牙权限未满足，请打开App授权");return false;}
  Intent i=new Intent(c,ReminderService.class).setAction("RECOVER").putExtra("recovery_reason",why);
  if(phoneState>=0)i.putExtra("phone_state",phoneState).putExtra("phone_at",phoneAt);
  try{c.startForegroundService(i);record(c,"已请求恢复："+why+"（等待服务启动）");return true;}
  catch(RuntimeException e){blocked(c,"后台恢复被阻止："+e.getClass().getSimpleName()+"；请打开App恢复监听");return false;}
 }
 public static void started(Context c,String why){cancel(c);c.getSystemService(NotificationManager.class).cancel(2);record(c,"监听服务已启动："+why);if(!"用户开启监听".equals(why)&&!"用户打开App后恢复".equals(why))prefs(c).edit().putString("last_automatic_start",new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date())+" "+why).apply();}
 public static void blocked(Context c,String why){
  record(c,why);prefs(c).edit().putString("last_blocked",new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date())+" "+why).commit();
  long now=System.currentTimeMillis(),last=prefs(c).getLong("last_notice",0);if(now>=last&&now-last<60000)return;
  prefs(c).edit().putLong("last_notice",now).apply();
  try{NotificationManager n=c.getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel("recovery","监听恢复提示",NotificationManager.IMPORTANCE_DEFAULT));
   n.notify(2,new Notification.Builder(c,"recovery").setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("收音机提醒：需要恢复监听").setContentText("请打开App。"+why).setStyle(new Notification.BigTextStyle().bigText(why+"。未运行期间无法保证来电提醒。"))
    .setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setAutoCancel(true).build());
  }catch(RuntimeException ignored){/* Android may also block notifications; the persisted message remains visible in-app. */}
 }
}
