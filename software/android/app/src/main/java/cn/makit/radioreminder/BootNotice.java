package cn.makit.radioreminder;
import android.content.*;
public final class BootNotice extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  String a=i.getAction();if(!Intent.ACTION_BOOT_COMPLETED.equals(a)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(a))return;
  if(Intent.ACTION_BOOT_COMPLETED.equals(a))MonitoringRecovery.prefs(c).edit().remove("last_broadcast_state").remove("last_broadcast_at").apply();
  if(!MonitoringRecovery.enabled(c))return;
  MonitoringRecovery.start(c,Intent.ACTION_BOOT_COMPLETED.equals(a)?"手机重启后恢复":"App升级后恢复",-1,0);
 }
}
