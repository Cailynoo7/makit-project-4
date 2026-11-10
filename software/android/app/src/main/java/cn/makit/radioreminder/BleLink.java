package cn.makit.radioreminder;
import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.os.*;
import java.util.*;
/** One GATT operation at a time. No alert is replayed after reconnection. */
@SuppressWarnings("deprecation")
public final class BleLink {
 public interface Listener{void state(String s,boolean ready);void status(byte[] b);void found(BluetoothDevice d);}
 private final Context ctx;private final Listener listener;private final Handler h=new Handler(Looper.getMainLooper());
 private BluetoothGatt gatt;private BluetoothGattCharacteristic command;private BluetoothLeScanner scanner;
 private boolean ready,closed;private long generation;private int sequence;private final ArrayDeque<byte[]> queue=new ArrayDeque<>();private boolean writing;
 private Runnable timeout,scanTimeout;private String address;
 public BleLink(Context c,Listener l){ctx=c;listener=l;}
 public boolean ready(){return ready;}
 public void scan(){
  if(ready){listener.state("已连接当前挂件；更换挂件前请先停止服务",true);return;}
  stopScan();BluetoothManager m=ctx.getSystemService(BluetoothManager.class);BluetoothAdapter a=m==null?null:m.getAdapter();
  if(a==null||!a.isEnabled()){listener.state("请先打开手机蓝牙",false);return;}
  scanner=a.getBluetoothLeScanner();
  scanner.startScan(Collections.singletonList(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(Protocol.SERVICE)).build()),new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);
  listener.state("正在查找挂件（20秒）",false);scanTimeout=()->{if(scanner!=null){stopScan();listener.state("查找结束；未找到时请开机并长按换音键3秒后重试",false);}};h.postDelayed(scanTimeout,20000);
 }
 private final ScanCallback scanCallback=new ScanCallback(){public void onScanResult(int type,ScanResult result){listener.found(result.getDevice());}public void onScanFailed(int code){listener.state("蓝牙扫描失败："+code,false);}};
 public void stopScan(){if(scanTimeout!=null)h.removeCallbacks(scanTimeout);if(scanner!=null){try{scanner.stopScan(scanCallback);}catch(SecurityException ignored){}scanner=null;}}
 public void connect(String mac){
  generation++;closed=false;address=mac;stopScan();closeGatt();
  try{BluetoothManager m=ctx.getSystemService(BluetoothManager.class);BluetoothAdapter a=m==null?null:m.getAdapter();
   if(a==null||!a.isEnabled()){listener.state("蓝牙未开启；来电监听继续，等待蓝牙恢复",false);reconnect();return;}
   listener.state("正在连接挂件",false);BluetoothDevice d=a.getRemoteDevice(mac);if(d.getBondState()==BluetoothDevice.BOND_NONE)d.createBond();gatt=d.connectGatt(ctx,false,callback,BluetoothDevice.TRANSPORT_LE);armTimeout();
  }catch(SecurityException e){closed=true;listener.state("蓝牙权限不可用，请打开App重新授权",false);}
  catch(RuntimeException e){listener.state("暂时无法连接挂件；稍后重试",false);reconnect();}
 }
 private void armTimeout(){if(timeout!=null)h.removeCallbacks(timeout);timeout=()->{listener.state("连接超时，稍后重试",false);reconnect();};h.postDelayed(timeout,20000);}
 private void reconnect(){if(closed)return;long token=++generation;closeGatt();h.postDelayed(()->{if(!closed&&token==generation&&address!=null)connect(address);},10000);}
 private void closeGatt(){ready=false;command=null;queue.clear();writing=false;if(timeout!=null)h.removeCallbacks(timeout);if(gatt!=null){try{gatt.disconnect();}catch(RuntimeException ignored){}try{gatt.close();}catch(RuntimeException ignored){}gatt=null;}}
 public void close(){generation++;closed=true;stopScan();closeGatt();}
 public void send(int op,int arg){if(!ready)return;byte[] b=Protocol.packet(op,++sequence,arg);if(queue.size()>16){listener.state("蓝牙指令拥堵，重连",false);reconnect();return;}queue.add(b);drain();}
 private void drain(){if(writing||queue.isEmpty()||gatt==null||command==null)return;writing=true;command.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);command.setValue(queue.peek());if(!gatt.writeCharacteristic(command)){writing=false;reconnect();}else armTimeout();}
 private final BluetoothGattCallback callback=new BluetoothGattCallback(){
  public void onConnectionStateChange(BluetoothGatt g,int status,int state){h.post(()->{if(g!=gatt)return;if(status==0&&state==BluetoothProfile.STATE_CONNECTED){g.discoverServices();armTimeout();}else{listener.state("挂件已断开；重连中",false);reconnect();}});}
  public void onServicesDiscovered(BluetoothGatt g,int status){h.post(()->{if(g!=gatt)return;BluetoothGattService s=g.getService(Protocol.SERVICE);if(status!=0||s==null){listener.state("挂件固件不匹配",false);close();return;}command=s.getCharacteristic(Protocol.COMMAND);BluetoothGattCharacteristic st=s.getCharacteristic(Protocol.STATUS);if(command==null||st==null){close();return;}g.setCharacteristicNotification(st,true);BluetoothGattDescriptor d=st.getDescriptor(Protocol.CCCD);if(d==null){close();return;}d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);g.writeDescriptor(d);armTimeout();});}
  public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){h.post(()->{if(g!=gatt)return;if(status!=0){listener.state("订阅失败，请重新配对",false);reconnect();return;}if(timeout!=null)h.removeCallbacks(timeout);ready=true;sequence=0;listener.state("挂件已连接",true);});}
  public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){h.post(()->{if(g!=gatt)return;if(timeout!=null)h.removeCallbacks(timeout);if(status!=0){listener.state("指令发送失败，重连",false);reconnect();return;}if(!queue.isEmpty())queue.remove();writing=false;drain();});}
  public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] b=c.getValue().clone();h.post(()->{if(g==gatt)listener.status(b);});}
 };
}
