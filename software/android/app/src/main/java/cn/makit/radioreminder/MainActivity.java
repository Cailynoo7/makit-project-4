package cn.makit.radioreminder;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.view.*;
import android.widget.*;
import java.util.*;
public final class MainActivity extends Activity {
 static final String FOUND="cn.makit.radioreminder.FOUND";
 final Handler h=new Handler(Looper.getMainLooper());TextView status,history;LinearLayout devices;
 final Set<String> addresses=new HashSet<>();
 final Runnable refresh=new Runnable(){public void run(){render();h.postDelayed(this,1000);}};
 final BroadcastReceiver found=new BroadcastReceiver(){public void onReceive(Context c,Intent i){String mac=i.getStringExtra("address");if(mac!=null&&addresses.add(mac)){Button b=new Button(MainActivity.this);b.setText("连接收音机挂件 · "+mac.substring(12));b.setOnClickListener(v->{new AlertDialog.Builder(MainActivity.this).setTitle("连接这个挂件？").setMessage("请只选择自己手边的挂件。首次连接可能需要确认系统蓝牙配对。验证版请一次只开启一个新挂件。").setPositiveButton("连接",(d,w)->start("CONNECT",mac)).setNegativeButton("取消",null).show();});devices.addView(b);}}};
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout box,String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(Color.rgb(32,36,40));v.setPadding(0,dp(7),0,dp(7));box.addView(v);return v;}
 void button(LinearLayout box,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setTextSize(18);b.setAllCaps(false);b.setMinHeight(dp(52));b.setOnClickListener(v->{try{action.run();}catch(Exception e){Toast.makeText(this,"操作未完成，请检查权限和蓝牙："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}});box.addView(b,new LinearLayout.LayoutParams(-1,-2));}
 public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(Color.rgb(242,244,246));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(40),dp(20),dp(32));box.setBackgroundColor(Color.rgb(247,248,250));scroll.setFillViewport(true);scroll.addView(box);setContentView(scroll);
  text(box,"收音机提醒",30);text(box,"验证版 0.4.0 · 由子女首次设置",16);
  status=text(box,"读取连接状态…",18);
  text(box,"① 首次授权",21);
  text(box,"仅在手机本地判断漏接，不上传号码、联系人或通话记录。需要电话状态、附近设备及通知读取权限。Android 8–11 扫描蓝牙还需定位权限及系统定位开关。",16);
  button(box,"授予电话与蓝牙权限",this::permissions);
  button(box,"允许读取系统未接通知",()->openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
  button(box,"可选：授权通话记录，核验未接/拒接",()->{new AlertDialog.Builder(this).setTitle("仅本地核验通话结果").setMessage("只查询记录ID、时间及类型，不查询号码和联系人。部分手机不允许直接安装的App获得此权限；若不允许，可选择下面的通知兼容模式。").setPositiveButton("授权",(d,w)->requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},102)).setNegativeButton("取消",null).show();});
  Switch compatibility=new Switch(this);compatibility.setText("启用通知兼容模式（可能误计拒接）");compatibility.setTextSize(17);compatibility.setPadding(0,dp(12),0,dp(12));compatibility.setChecked(getSharedPreferences("settings",0).getBoolean("compatible",false));compatibility.setOnCheckedChangeListener((v,on)->{getSharedPreferences("settings",0).edit().putBoolean("compatible",on).apply();if(ReminderService.live!=null)ReminderService.live.stopAlert("判断模式已切换，重新计数");});box.addView(compatibility);
  text(box,"严格模式需要通话记录权限；未获得时不会自动计数。兼容模式交叉检查来电与系统未接通知，但部分手机会把拒接也标成未接。",15);
  text(box,"② 来电历史与连接",21);
  button(box,"查看来电历史（按日期保存）",()->startActivity(new Intent(this,CallHistoryActivity.class)));
  button(box,"开始来电监听",()->start("START",null));
  text(box,"完成电话与蓝牙权限授权后可单独启动监听，无需挂件在线。历史保存在本机，重启后仍能查看；开启后会请求清理/重启恢复；系统强行停止或厂商拦截时仍需手动打开App。",15);
  button(box,"查找附近挂件",()->{addresses.clear();devices.removeAllViews();start("SCAN",null);});
  devices=new LinearLayout(this);devices.setOrientation(1);box.addView(devices);
  button(box,"恢复已保存的连接",()->start("START",null));
  button(box,"打开应用设置，检查后台限制",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));
  button(box,"打开小米自启动设置",()->{Intent i=new Intent("miui.intent.action.OP_AUTO_START");if(i.resolveActivity(getPackageManager())!=null)startActivity(i);else{Toast.makeText(this,"此系统没有该入口，请在应用信息中检查自启动",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}});
  text(box,"小米需允许自启动与后台运行。划掉任务会尝试恢复，但一键清理/省电策略仍可能阻止。强行停止后请手动打开App；恢复成功也不能补回断连期间的提醒。",15);
  text(box,"③ 声音与停止提醒",21);
  text(box,"前面只保留一个三角键：待机短按换音，提醒中短按停止；长按3秒配对。",16);
  button(box,"试听一次（遵守夜间静音）",()->service().preview());
  button(box,"我已查看手机 · 停止提醒",()->service().stopAlert("用户在App中确认已查看"));
  button(box,"切换叮咚 / 叮叮",()->{int s=1-getSharedPreferences("settings",0).getInt("sound",0);getSharedPreferences("settings",0).edit().putInt("sound",s).apply();service().link.send(Protocol.SOUND,s);});
  button(box,"切换音量：低 / 中 / 高",()->{int s=(getSharedPreferences("settings",0).getInt("volume",0)+1)%3;getSharedPreferences("settings",0).edit().putInt("volume",s).apply();service().link.send(Protocol.VOLUME,s);});
  text(box,"22:00–06:00不发声；没有有效时间或蓝牙断开时不补响。手机仅亮屏或解锁不会自动确认已查看。",15);
  text(box,"④ 验证与运行记录",21);
  button(box,"模拟一通漏接（点三次触发）",()->{new AlertDialog.Builder(this).setTitle("模拟漏接测试").setMessage("仅模拟漏接事件，仍遵守当前手机静音/勿扰及夜间规则。先把手机设为静音；每点一次计一通。此测试不能证明真实来电识别正常。").setPositiveButton("模拟一通",(d,w)->service().simulate()).setNegativeButton("取消",null).show();});
  button(box,"清空计数与停止提醒",()->service().stopAlert("手动清空测试与漏接序列"));
  history=text(box,"暂无运行记录",14);
  button(box,"停止监听与自动恢复",()->{MonitoringRecovery.setEnabled(this,false);if(ReminderService.live!=null)start("STOP",null);});
  IntentFilter f=new IntentFilter(FOUND);if(Build.VERSION.SDK_INT>=33)registerReceiver(found,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(found,f);
 }
 ReminderService service(){if(ReminderService.live==null)throw new IllegalStateException("请先连接挂件");return ReminderService.live;}
 void openSettings(String action){startActivity(new Intent(action));}
 void permissions(){ArrayList<String> list=new ArrayList<>();list.add(Manifest.permission.READ_PHONE_STATE);if(Build.VERSION.SDK_INT>=31){list.add(Manifest.permission.BLUETOOTH_SCAN);list.add(Manifest.permission.BLUETOOTH_CONNECT);}else list.add(Manifest.permission.ACCESS_FINE_LOCATION);if(Build.VERSION.SDK_INT>=33)list.add(Manifest.permission.POST_NOTIFICATIONS);requestPermissions(list.toArray(new String[0]),101);}
 boolean granted(String p){return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
 boolean bleAllowed(){return Build.VERSION.SDK_INT>=31?granted(Manifest.permission.BLUETOOTH_SCAN)&&granted(Manifest.permission.BLUETOOTH_CONNECT):granted(Manifest.permission.ACCESS_FINE_LOCATION);}
 void start(String action,String address){if(!"STOP".equals(action)&&(!bleAllowed()||!granted(Manifest.permission.READ_PHONE_STATE))){Toast.makeText(this,"请先完成电话与蓝牙权限授权",Toast.LENGTH_LONG).show();permissions();return;}if(!"STOP".equals(action))MonitoringRecovery.setEnabled(this,true);Intent i=new Intent(this,ReminderService.class).setAction(action);if(address!=null)i.putExtra("address",address);startForegroundService(i);}
 void render(){String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");boolean n=enabled!=null&&enabled.contains(getPackageName()+"/");String mode=granted(Manifest.permission.READ_CALL_LOG)?"通话记录核验":getSharedPreferences("settings",0).getBoolean("compatible",false)?"通知推断（有误报风险）":"未启用自动判断";status.setText((ReminderService.live==null?"来电监听：未启动\n":"来电监听：运行中\n")+ReminderService.connection+"\n"+ReminderService.deviceStatus+"\n"+mode+"\n通知读取："+(n?"已授权":"未授权"));if(ReminderService.live!=null){ReminderService service=ReminderService.live;java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA);status.append("\n本次监听启动："+fmt.format(new Date(service.startedWall))+"\n最近电话状态更新："+(service.lastPhoneUpdate==0?"尚未收到":fmt.format(new Date(service.lastPhoneUpdate))));}status.append("\n自动恢复："+(MonitoringRecovery.enabled(this)?"已启用（仍受系统限制）":"未启用")+"\n最近恢复情况："+MonitoringRecovery.summary(this));history.setText(ReminderService.logs.isEmpty()?"暂无运行记录":android.text.TextUtils.join("\n",ReminderService.logs));}
 protected void onResume(){super.onResume();if(MonitoringRecovery.enabled(this)&&ReminderService.live==null)MonitoringRecovery.start(this,"用户打开App后恢复",-1,0);h.post(refresh);if(ReminderService.live!=null){ReminderService.live.ensureObserver();ReminderService.live.refreshHistory();}if(ReminderService.live!=null)NotificationListenerService.requestRebind(new ComponentName(this,CallNotifications.class));}
 protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 protected void onDestroy(){unregisterReceiver(found);super.onDestroy();}
}
