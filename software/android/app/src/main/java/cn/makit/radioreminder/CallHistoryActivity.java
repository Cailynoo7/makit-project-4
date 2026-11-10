package cn.makit.radioreminder;
import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
/** History can be viewed offline, without starting the BLE/call service. */
public final class CallHistoryActivity extends Activity {
 CallHistoryDb db;LinearLayout rows;TextView info;int shown;String lastDay="";
 final Handler h=new Handler(Looper.getMainLooper());
 final Runnable refresh=new Runnable(){public void run(){reload();h.postDelayed(this,5000);}};
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout box,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(32,36,40));t.setPadding(0,dp(8),0,dp(8));box.addView(t);return t;}
 void button(LinearLayout box,String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());box.addView(b);}
 public void onCreate(Bundle state){super.onCreate(state);db=new CallHistoryDb(this);if(ReminderService.live==null)db.recoverInterrupted();
  ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(24),dp(20),dp(24));scroll.addView(box);setContentView(scroll);
  text(box,"来电历史",28);
  text(box,"按手机当前时区显示来电开始时间。只保存在本机，最多1000条；关闭App或重启手机后仍可查看。不保存号码和联系人。",16);
  text(box,"仅记录监听运行期间检测到的普通电话，不补录服务停止期间或安装前的电话。结果无法确认时会明确标注。系统记录补读只用于历史，不能证明后台实时监听成功；不会触发补响。",15);
  button(box,"返回",this::finish);
  button(box,"刷新来电历史",this::reload);
  button(box,"清空来电历史",()->new AlertDialog.Builder(this).setTitle("清空本机来电历史？").setMessage("删除后无法恢复；不会删除手机拨号器里的通话记录。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{db.clear();reload();}).show());
  info=text(box,"",15);rows=new LinearLayout(this);rows.setOrientation(1);box.addView(rows);
  button(box,"显示更早记录",()->{h.removeCallbacks(refresh);append();});
 }
 void reload(){if(ReminderService.live!=null)ReminderService.live.refreshHistory();shown=0;lastDay="";rows.removeAllViews();append();}
 void append(){
  List<CallHistoryDb.Row> page=db.list(shown,100);
  SimpleDateFormat day=new SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA),time=new SimpleDateFormat("HH:mm:ss",Locale.CHINA);
  for(CallHistoryDb.Row r:page){Date d=new Date(r.time);String group=day.format(d);if(!group.equals(lastDay)){text(rows,group,22);lastDay=group;}text(rows,time.format(d)+"  "+CallHistoryState.label(r.result)+"\n"+r.evidence+"\n"+CallObservation.label(r.liveSeen)+(r.firstSaved==null?"":"\n首次保存："+new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date(r.firstSaved))),18);}
  shown+=page.size();info.setText(shown==0?"暂无来电历史。完成权限授权后，在首页启动来电监听。":"已显示 "+shown+" 条；较新的来电在前。"+(page.size()<100?" 已显示全部保存记录。":""));
 }
 protected void onResume(){super.onResume();h.post(refresh);}
 protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 protected void onDestroy(){h.removeCallbacks(refresh);db.close();super.onDestroy();}
}
