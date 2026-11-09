package cn.makit.radioreminder;
import android.app.*;
import android.content.pm.*;
import android.service.notification.*;
import android.telecom.TelecomManager;
public final class CallNotifications extends NotificationListenerService {
 boolean trusted(StatusBarNotification s){
  String pkg=s.getPackageName();String dialer=getSystemService(TelecomManager.class).getDefaultDialerPackage();
  if(pkg.equals(dialer))return true;
  try{return (getPackageManager().getApplicationInfo(pkg,0).flags&ApplicationInfo.FLAG_SYSTEM)!=0;}catch(PackageManager.NameNotFoundException e){return false;}
 }
 public void onNotificationPosted(StatusBarNotification s){if(ReminderService.live!=null&&trusted(s)&&Notification.CATEGORY_MISSED_CALL.equals(s.getNotification().category))ReminderService.live.missedNotification(s.getKey(),s.getPostTime());}
 public void onNotificationRemoved(StatusBarNotification s,RankingMap map,int reason){if(ReminderService.live!=null&&trusted(s)&&Notification.CATEGORY_MISSED_CALL.equals(s.getNotification().category)&&reason==REASON_CLICK)ReminderService.live.stopAlert("已点击系统未接通知");}
}
