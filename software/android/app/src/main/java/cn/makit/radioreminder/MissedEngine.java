package cn.makit.radioreminder;
import java.util.*;
/** Pure monotonic-time decision engine; no phone numbers or Android dependencies. */
public final class MissedEngine {
 public static final long WINDOW=600_000L;
 private final ArrayDeque<Long> calls=new ArrayDeque<>();
 private final LinkedHashSet<String> seen=new LinkedHashSet<>();
 private boolean connected,alert;
 public void connected(boolean value){if(connected!=value)reset();connected=value;}
 public void reset(){calls.clear();alert=false;}
 public boolean active(){return alert;}
 public int count(long now){expire(now);return calls.size();}
 private void expire(long now){while(!calls.isEmpty() && now-calls.peekFirst()>WINDOW)calls.removeFirst();}
 public boolean missed(String id,long now,Boolean silent,boolean quiet){
  if(!connected||seen.contains(id))return false;
  seen.add(id);if(seen.size()>128)seen.remove(seen.iterator().next());
  expire(now);calls.addLast(now);
  if(calls.size()>3)calls.removeFirst();
  if(!alert && calls.size()>=3 && !Boolean.FALSE.equals(silent) && !quiet){alert=true;return true;}
  return false;
 }
 public static boolean quiet(int minute){return minute>=22*60||minute<6*60;}
}
