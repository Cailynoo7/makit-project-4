import cn.makit.radioreminder.CallHistoryState;
public class CallHistoryStateTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  String r=CallHistoryState.RINGING,u=CallHistoryState.UNKNOWN,a=CallHistoryState.ANSWERED,m=CallHistoryState.MISSED,j=CallHistoryState.REJECTED,s=CallHistoryState.SUSPECTED;
  check(CallHistoryState.merge(r,a,false).equals(a),"ring to offhook answers incoming");
  check(CallHistoryState.merge(a,u,false).equals(a),"idle does not erase answered");
  check(CallHistoryState.merge(r,u,false).equals(u),"idle alone cannot prove missed");
  check(CallHistoryState.merge(u,s,false).equals(s),"correlated notification remains uncertain");
  check(CallHistoryState.merge(s,u,false).equals(s),"later interruption preserves suspicion");
  check(CallHistoryState.merge(s,j,true).equals(j),"system rejection overrides suspected missed");
  check(CallHistoryState.merge(a,m,true).equals(m),"system evidence wins phone-state observation");
  check(CallHistoryState.merge(m,s,false).equals(m),"notification cannot downgrade confirmed result");
  check(CallHistoryState.merge(j,a,false).equals(j),"late state cannot override system rejection");
  check(CallHistoryState.merge(a,s,false).equals(a),"answered is not inferred missed");
  check(CallHistoryState.label(u).contains("未确认"),"unknown explicitly displayed");
  check(CallHistoryState.label(s).contains("可能包含拒接"),"uncertainty shown to user");
  check(CallHistoryState.label(m).contains("系统已核验"),"system evidence displayed");
  check(!CallHistoryState.valid("outgoing"),"outgoing not a recorded incoming result");
  try{CallHistoryState.merge(u,"bad",false);throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){checks++;}
  System.out.println("CallHistoryStateTest: "+checks+" checks passed");
 }
}
