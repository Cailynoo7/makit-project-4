package cn.makit.radioreminder;
import android.content.*;
/** Single, explicit inexact alarm. A denial is reported instead of retried in a loop. */
public final class RecoveryReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  if(!MonitoringRecovery.ALARM.equals(i.getAction())||!MonitoringRecovery.enabled(c))return;
  if(ReminderService.live!=null){MonitoringRecovery.record(c,"任务清理后的检查：监听服务仍在运行");return;}
  MonitoringRecovery.start(c,"任务清理后的恢复检查",-1,0);
 }
}
