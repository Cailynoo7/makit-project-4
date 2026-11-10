package cn.makit.radioreminder;
/** Result policy shared by the database and desktop tests. No numbers are stored. */
public final class CallHistoryState {
 public static final String RINGING="ringing", UNKNOWN="unknown", ANSWERED="answered", MISSED="missed", REJECTED="rejected", SUSPECTED="suspected";
 public static boolean valid(String s){return RINGING.equals(s)||UNKNOWN.equals(s)||ANSWERED.equals(s)||MISSED.equals(s)||REJECTED.equals(s)||SUSPECTED.equals(s);}
 public static String merge(String old,String next,boolean authoritative){
  if(!valid(old)||!valid(next))throw new IllegalArgumentException("Unknown call result");
  if(authoritative)return next;
  if(MISSED.equals(old)||REJECTED.equals(old)||ANSWERED.equals(old))return old;
  if(SUSPECTED.equals(old)&&UNKNOWN.equals(next))return old;
  return next;
 }
 public static String label(String s){
  if(RINGING.equals(s))return "来电响铃中";
  if(ANSWERED.equals(s))return "已接听";
  if(MISSED.equals(s))return "未接听（系统已核验）";
  if(REJECTED.equals(s))return "已拒接（系统已核验）";
  if(SUSPECTED.equals(s))return "疑似漏接（通知推断，可能包含拒接）";
  return "已结束，接听结果未确认";
 }
}
