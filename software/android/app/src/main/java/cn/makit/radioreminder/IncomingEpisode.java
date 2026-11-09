package cn.makit.radioreminder;
/** Conservative notification correlation; it cannot prove rejection vs timeout. */
public final class IncomingEpisode {
 private long token,startWall,endedAt=-1;private boolean pending;
 public void ring(long wall,long monotonic){token=monotonic+1;startWall=wall;endedAt=-1;pending=false;}
 public void clear(){token=0;endedAt=-1;pending=false;}
 public boolean idle(long monotonic){if(token==0)return false;endedAt=monotonic;return true;}
 public boolean notification(long postedWall,long wallNow){
  if(token==0||postedWall<startWall||wallNow-postedWall>90000||postedWall-wallNow>5000)return false;
  pending=true;return true;
 }
 public String consume(long monotonic){
  if(token==0||!pending||endedAt<0||monotonic-endedAt>90000)return null;
  String id="notification:"+token;clear();return id;
 }
}
