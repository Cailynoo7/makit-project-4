package cn.makit.radioreminder;
/** A system call-log row is evidence of a call, not evidence that our service saw it live. */
public final class CallObservation {
 public static final long MATCH_MS=3000;
 public static boolean canAlert(boolean liveSeen,long callTime,long serviceStart,long connectedTime,long now){
  return liveSeen&&callTime>=serviceStart&&callTime>=connectedTime&&callTime<=now+5000&&now-callTime<=180000;
 }
 public static String label(Integer liveSeen){
  if(liveSeen==null)return "旧版记录：无法确认当时是否实时检测";
  return liveSeen==1?"实时检测到响铃（结果来源见上方）":"系统记录补读：不代表当时实时检测成功";
 }
}
