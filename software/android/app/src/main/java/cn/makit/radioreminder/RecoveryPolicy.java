package cn.makit.radioreminder;
/** Pure policy: user stop always wins over alarms, phone broadcasts and OS restarts. */
public final class RecoveryPolicy {
 public static boolean mayRecover(boolean enabled,boolean permissions){return enabled&&permissions;}
 public static boolean freshPhoneEvent(long sent,long now){return sent>0&&now>=sent&&now-sent<=10000;}
 public static boolean duplicatePhoneEvent(int state,int previous,long now,long last){return state==previous&&last>0&&now>=last&&now-last<1500;}
 public static int phoneState(String value){if("RINGING".equals(value))return 1;if("OFFHOOK".equals(value))return 2;if("IDLE".equals(value))return 0;return -1;}
}
