import cn.makit.radioreminder.*;
public class RecoveryPolicyTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  check(!RecoveryPolicy.mayRecover(false,true),"user stop wins");
  check(!RecoveryPolicy.mayRecover(true,false),"permissions gate");
  check(RecoveryPolicy.mayRecover(true,true),"allowed restore");
  check(!RecoveryPolicy.mayRecover(false,false),"fresh install stays off");
  check(RecoveryPolicy.freshPhoneEvent(1,1),"fresh cold start event");
  check(RecoveryPolicy.freshPhoneEvent(1,10001),"10s inclusive");
  check(!RecoveryPolicy.freshPhoneEvent(1,10002),"stale event not replayed as live");
  check(!RecoveryPolicy.freshPhoneEvent(0,1),"missing event not fabricated");
  check(!RecoveryPolicy.freshPhoneEvent(100,99),"reboot or future time rejected");
  check(RecoveryPolicy.phoneState("RINGING")==1,"ring mapped");
  check(RecoveryPolicy.phoneState("OFFHOOK")==2,"offhook mapped");
  check(RecoveryPolicy.phoneState("IDLE")==0,"idle mapped");
  check(RecoveryPolicy.phoneState(null)==-1&&RecoveryPolicy.phoneState("bad")==-1,"malformed broadcast ignored");
  check(RecoveryPolicy.duplicatePhoneEvent(1,1,2000,1000),"duplicate cold broadcast suppressed");
  check(!RecoveryPolicy.duplicatePhoneEvent(2,1,2000,1000),"new state not suppressed");
  check(!RecoveryPolicy.duplicatePhoneEvent(1,1,3000,1000),"later separate call not suppressed");
  check(!RecoveryPolicy.duplicatePhoneEvent(1,1,500,1000),"monotonic reset not suppressed");
  // Duplicate broadcasts and the normal phone listener share the episode state gate.
  IncomingEpisode e=new IncomingEpisode();e.ring(10000,100);e.idle(200);e.notification(10001,10002);
  check(e.consume(201)!=null&&e.consume(202)==null,"one ended episode cannot be consumed twice");
  System.out.println("RecoveryPolicyTest: "+checks+" checks passed");
 }
}
