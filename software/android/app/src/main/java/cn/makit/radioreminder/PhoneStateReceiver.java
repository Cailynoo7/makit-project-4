package cn.makit.radioreminder;
import android.content.*;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
/** PHONE_STATE is a manifest broadcast exception. It can wake the process when MIUI permits. */
public final class PhoneStateReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  if(!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(i.getAction())||!MonitoringRecovery.enabled(c))return;
  // Deliberately ignore EXTRA_INCOMING_NUMBER. Some Android versions send the state twice.
  int state=RecoveryPolicy.phoneState(i.getStringExtra(TelephonyManager.EXTRA_STATE));if(state<0)return;
  if(ReminderService.live!=null)return; // The live PhoneStateListener owns events; do not feed a second state stream.
  long now=SystemClock.elapsedRealtime();SharedPreferences p=MonitoringRecovery.prefs(c);
  if(RecoveryPolicy.duplicatePhoneEvent(state,p.getInt("last_broadcast_state",-1),now,p.getLong("last_broadcast_at",0)))return;
  p.edit().putInt("last_broadcast_state",state).putLong("last_broadcast_at",now).commit();
  MonitoringRecovery.start(c,"电话状态广播唤起",state,now);
 }
}
