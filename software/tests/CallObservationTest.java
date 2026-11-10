import cn.makit.radioreminder.*;
public class CallObservationTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 public static void main(String[] args){
  check(!CallObservation.canAlert(false,10000,1000,1000,11000),"backfill cannot alert");
  check(CallObservation.canAlert(true,10000,1000,1000,11000),"live current call eligible");
  check(!CallObservation.canAlert(true,999,1000,0,11000),"previous service not replayed");
  check(!CallObservation.canAlert(true,10000,1000,10001,11000),"before reconnect not replayed");
  check(!CallObservation.canAlert(true,10000,1000,1000,190001),"old live event expires");
  check(CallObservation.canAlert(true,10000,1000,1000,190000),"freshness inclusive boundary");
  check(!CallObservation.canAlert(true,20000,1000,1000,10000),"future date rejected");
  check(CallObservation.label(null).contains("无法确认"),"legacy rows not invented live");
  check(CallObservation.label(0).contains("补读"),"backfill explicit");
  check(CallObservation.label(1).contains("实时"),"live explicit");
  MissedEngine e=new MissedEngine();e.connected(true);
  for(int i=0;i<3;i++)if(CallObservation.canAlert(false,10000+i,1000,1000,11000))e.missed("old:"+i,i,true,false);
  check(e.count(11000)==0&&!e.active(),"three backfilled missed calls do not advance reminder");
  System.out.println("CallObservationTest: "+checks+" checks passed");
 }
}
