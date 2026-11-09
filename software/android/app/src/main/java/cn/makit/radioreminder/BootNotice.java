package cn.makit.radioreminder;
import android.content.*;
import android.app.*;
public final class BootNotice extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))return;ReminderService.createChannel(c);c.getSystemService(NotificationManager.class).notify(2,ReminderService.notice(c,"手机重启后，请点此恢复挂件连接"));}
}
