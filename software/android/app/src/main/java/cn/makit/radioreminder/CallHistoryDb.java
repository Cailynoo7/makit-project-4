package cn.makit.radioreminder;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
/** Private on-device storage. No phone number, contact or notification body columns. */
public final class CallHistoryDb extends SQLiteOpenHelper {
 public static final int MAX_RECORDS=1000;
 public static final String CREATE="CREATE TABLE calls (_id INTEGER PRIMARY KEY AUTOINCREMENT, started_at INTEGER NOT NULL, result TEXT NOT NULL, evidence TEXT NOT NULL, system_id TEXT UNIQUE, live_seen INTEGER, first_saved_at INTEGER)";
 public static final String PRUNE="DELETE FROM calls WHERE _id NOT IN (SELECT _id FROM calls ORDER BY started_at DESC, _id DESC LIMIT 1000)";
 public static final class Row {public long id,time;public Long firstSaved;public Integer liveSeen;public String result,evidence;}
 public CallHistoryDb(Context c){super(c.getApplicationContext(),"call-history.db",null,2);}
 public void onCreate(SQLiteDatabase db){db.execSQL(CREATE);db.execSQL("CREATE INDEX calls_time ON calls(started_at DESC)");}
 public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion==1&&newVersion==2){db.execSQL("ALTER TABLE calls ADD COLUMN live_seen INTEGER");db.execSQL("ALTER TABLE calls ADD COLUMN first_saved_at INTEGER");}else throw new IllegalStateException("Database migration required");}
 public synchronized long begin(long when){
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try{ContentValues v=new ContentValues();v.put("started_at",when);v.put("result",CallHistoryState.RINGING);v.put("evidence","手机来电状态");v.put("live_seen",1);v.put("first_saved_at",System.currentTimeMillis());long id=db.insertOrThrow("calls",null,v);db.execSQL(PRUNE);db.setTransactionSuccessful();return id;}finally{db.endTransaction();}
 }
 public synchronized void result(long id,String next,String evidence){
  if(id<=0)return;
  SQLiteDatabase db=getWritableDatabase();
  try(Cursor c=db.query("calls",new String[]{"result","system_id"},"_id=?",new String[]{Long.toString(id)},null,null,null)){
   if(!c.moveToFirst()||!c.isNull(1))return; // System call-log results have final authority.
   String old=c.getString(0),merged=CallHistoryState.merge(old,next,false);
   if(merged.equals(old))return;
   ContentValues v=new ContentValues();v.put("result",merged);v.put("evidence",evidence);db.update("calls",v,"_id=?",new String[]{Long.toString(id)});
  }
 }
 public synchronized void recoverInterrupted(){
  ContentValues v=new ContentValues();v.put("result",CallHistoryState.UNKNOWN);v.put("evidence","监听曾中断，无法确认最终结果");getWritableDatabase().update("calls",v,"result=?",new String[]{CallHistoryState.RINGING});
 }
 public synchronized boolean system(String systemId,long when,String result){
  if(!CallHistoryState.valid(result))throw new IllegalArgumentException("result");
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try{
   long id=-1;boolean liveSeen=false;
   try(Cursor c=db.query("calls",new String[]{"_id","live_seen"},"system_id=?",new String[]{systemId},null,null,null)){if(c.moveToFirst()){id=c.getLong(0);liveSeen=!c.isNull(1)&&c.getInt(1)==1;}}
   if(id<0)try(Cursor c=db.query("calls",new String[]{"_id","live_seen"},"system_id IS NULL AND (live_seen=1 OR live_seen IS NULL) AND started_at BETWEEN ? AND ?",new String[]{Long.toString(when-CallObservation.MATCH_MS),Long.toString(when+CallObservation.MATCH_MS)},null,null,"ABS(started_at-"+when+") ASC, _id DESC","1")){if(c.moveToFirst()){id=c.getLong(0);liveSeen=!c.isNull(1)&&c.getInt(1)==1;}}
   ContentValues v=new ContentValues();v.put("started_at",when);v.put("result",result);v.put("evidence","系统通话记录核验");v.put("system_id",systemId);
   if(id<0){v.put("live_seen",0);v.put("first_saved_at",System.currentTimeMillis());db.insertOrThrow("calls",null,v);}else db.update("calls",v,"_id=?",new String[]{Long.toString(id)});
   db.execSQL(PRUNE);db.setTransactionSuccessful();return liveSeen;
  }finally{db.endTransaction();}
 }
 public synchronized List<Row> list(int offset,int limit){
  ArrayList<Row> rows=new ArrayList<>();
  try(Cursor c=getReadableDatabase().query("calls",new String[]{"_id","started_at","result","evidence","live_seen","first_saved_at"},null,null,null,null,"started_at DESC, _id DESC",Math.max(0,offset)+","+Math.min(100,Math.max(1,limit)))){
   while(c.moveToNext()){Row r=new Row();r.id=c.getLong(0);r.time=c.getLong(1);r.result=c.getString(2);r.evidence=c.getString(3);r.liveSeen=c.isNull(4)?null:c.getInt(4);r.firstSaved=c.isNull(5)?null:c.getLong(5);rows.add(r);}
  }return rows;
 }
 public synchronized void clear(){getWritableDatabase().delete("calls",null,null);}
}
