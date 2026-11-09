import cn.makit.radioreminder.*;
public class EngineTest {
 static int checks;
 static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 static MissedEngine fresh(){MissedEngine e=new MissedEngine();e.connected(true);return e;}
 public static void main(String[] args){
  MissedEngine e=fresh();check(!e.missed("a",0,true,false),"one");check(!e.missed("a",1,true,false)&&e.count(1)==1,"dedupe");check(!e.missed("b",100,true,false),"two");check(e.missed("c",600000,true,false),"inclusive boundary");check(!e.missed("d",600001,true,false),"no repeat active");
  e=fresh();e.missed("a",0,true,false);e.missed("b",1,true,false);check(!e.missed("c",600001,true,false),"expired first");
  e=fresh();e.missed("a",0,false,false);e.missed("b",1,false,false);check(!e.missed("c",2,false,false),"normal ring suppress");
  e=fresh();e.missed("a",0,true,true);e.missed("b",1,true,true);check(!e.missed("c",2,true,true),"night");check(!e.active(),"no dawn backlog");
  e=fresh();e.missed("a",0,true,false);e.missed("b",1,true,false);e.reset();check(!e.missed("c",2,true,false),"answered clears");
  e=fresh();e.missed("a",0,true,false);e.missed("b",1,true,false);e.connected(false);check(!e.missed("c",2,true,false),"offline ignore");e.connected(true);check(!e.missed("d",3,true,false),"no replay on reconnect");
  e=fresh();e.missed("a",0,null,false);e.missed("b",1,null,false);check(e.missed("c",2,null,false),"unknown silent heuristic");
  check(MissedEngine.quiet(1320)&&MissedEngine.quiet(359)&&!MissedEngine.quiet(360)&&!MissedEngine.quiet(1319),"quiet boundaries");
  byte[] b=Protocol.packet(2,0x12345678,1234);check(b.length==10&&b[2]==0x78&&Protocol.arg(b)==1234,"wire format");
  IncomingEpisode ep=new IncomingEpisode();check(ep.consume(0)==null,"no spontaneous notification event");
  ep.ring(10000,100);check(!ep.notification(9999,10001),"old notification rejected");ep.idle(200);check(ep.consume(201)==null,"ring idle alone not missed");
  check(ep.notification(10002,10003),"fresh system notification");check(ep.consume(202)!=null,"fresh correlated event");check(ep.consume(203)==null,"merged refresh dedupe");
  ep.ring(20000,300);ep.notification(20001,20002);check(ep.consume(301)==null,"notification before idle waits");ep.idle(400);check(ep.consume(401)!=null,"notification before idle resolved");
  ep.ring(30000,500);ep.clear();check(!ep.notification(30001,30002),"answered or disconnected clears candidate");
  ep.ring(40000,600);ep.idle(700);ep.notification(40001,40002);check(ep.consume(90701)==null,"late candidate expires");
  System.out.println("EngineTest: "+checks+" checks passed");
 }
}
