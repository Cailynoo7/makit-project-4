# 收音机漏接提醒器软件验证版 0.2.0

原生 Java Android App + Seeed XIAO nRF52840 基础版 Arduino/Bluefruit 固件，严格沿用载板接线，不修改电子硬件或外壳。
可安装/烧录文件及中文说明在 `product/software-validation/v0.2.0`。这是首次功能联调包；编译和逻辑测试不代表真机验收。

## 运行范围

Android 最低26（8.0）、目标35（15），不含第三方分析SDK、不申请INTERNET、不查询号码或联系人。Android 16等新系统尚未测试。通知监听会收到系统交付的通知对象，但只处理默认拨号器/系统应用的 `CATEGORY_MISSED_CALL`，不读取标题、正文、号码，不存储通知内容。记录仅在内存保留30条状态事件。

App采用用户从前台启动的connectedDevice前台服务及低重要性常驻通知；已保存的挂件会断线重连，不重放旧提醒。手机重启后通知引导用户恢复服务，未实现免操作自启动。部分厂商仍需人工允许后台运行。后台被系统终止后不能保证持续监听。

严格模式使用可选READ_CALL_LOG权限，查询ID、DATE、TYPE三列；仅MISSED_TYPE计数，REJECTED_TYPE不计，已接/拨出清零。该权限在Android上属于hard restricted，部分直接安装渠道无法授予，App会保留“未启用自动判断”状态。

默认关闭通知兼容模式。用户显式开启后，以一次未接听的RINGING→IDLE事件和时间对应的系统未接通知交叉判断，通知合并更新不会独立增加计数。此路径不能可靠区分系统同样标记为未接的拒接，不宣称严格验收通过。Android 8–11等旧拨号器可能没有missed_call分类，可能漏报。没有权限、通知证据或已连接的挂件时不伪造确定事件。

模拟计数与真实计数隔离，遵守静音/勿扰和22–06静音规则；清空测试或在提醒中短按前面单键后恢复真实监听。模拟测试只能验证判断逻辑和BLE/音响链路。

## 固件行为

D0=PWM音频、D1=AMP_EN、D2=换音/停止复用、D3不接线且固件不监听、D4/5=音量±、D6=电源；全部按键低有效/30ms消抖。P0.13使用板型常量PIN_CHARGING_CURRENT（Arduino D22）置高选择低充电电流；不能误用D13（LED）。电池读取使用VBAT_ENABLE D14及PIN_VBAT D32，板型基本版1.1.13中已核对。

两种四音提示、3档幅度，默认最低档。PWM3 EasyDMA，64kHz载波/16kHz采样，约889/667Hz正弦提示，每音35ms起落；最高幅度60/250，在3.3V、11:1衰减和24dB典型功放增益下约0.16W/4Ω估算，真实输出/响度需示波器及声学测试。每10秒一轮、最多2分钟；手动停止、断连、时钟过期或夜间立即停止。手机30秒同步本地分钟；固件90秒时间租期，未同步不发声。夜间不补响。

前面三角单键移至原下方位置，待机短按换音试听；提醒中短按只停止并清零，不切换音色、不认定已查看手机；音量±为饱和3档。换音长按3秒重新开放60秒新手机配对窗口；开机初始也开放60秒。已配对手机可重连。BLE指令和状态要求加密，Just Works不具备MITM防护，验证版未进行安全审计。电源长按2秒、松开后System OFF，D6低电平唤醒，需持续按2秒才开机；不是切断电池。音色、音量延迟5秒写入内部Flash，关机前保存。

## 构建与检查

工具均在 `.work/software`，不全局安装。macOS arm64，JDK17.0.21、Android platform35 r02/build-tools35.0.0、Arduino CLI1.5.2-rc.1（实际使用版本）、Seeeduino:nrf52 1.1.13及依赖版本见toolchain-lock.json和实际编译记录。APK由aapt2、javac、D8、zipalign和apksigner生成，不依赖Gradle或外部Android运行库。

- `python3 software/tools/build_android.py`：构建APK、zipalign、v2/v3签名校验。签名为项目本地验证密钥，已在`.work`中保存；不是商店发布密钥。后续升级需保留同一密钥，丢失时需卸载后重装并重新授权。
- `python3 software/tools/build_firmware.py`：编译基本版XIAO应用，生成UF2/HEX和串口DFU包。应用从0x27000开始，需要S140 7.3.0兼容引导环境；不包含替换SoftDevice或引导程序。
- `python3 software/tools/run_tests.py`：纯Java决策/通知关联测试和C++固件状态机测试。

没有连接手机或载板进行安装运行、BLE收发、扬声器播放、按键唤醒或真实来电测试。桌面测试不验证Android后台调度、厂商拨号器通知、焊接/电气输出、音量、72小时续航或充电温升。

## 开发依据

- https://developer.android.com/develop/connectivity/bluetooth/ble/background
- https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device
- https://developer.android.com/reference/android/telephony/PhoneStateListener
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/reference/android/Manifest.permission#READ_CALL_LOG
- https://developer.android.com/reference/android/provider/CallLog.Calls
- https://wiki.seeedstudio.com/XIAO_BLE/
- https://files.seeedstudio.com/arduino/package_seeeduino_boards_index.json
- 固件API/引脚以下载并校验的Seeed nRF52 Boards 1.1.13源码为准。

四键版对应载板发布bbae20a434339443ea3c5a5be6bef92bbbb1d6c7cf2214563fcd01bb77dd2b27；仅1颗前面开关、2颗顶键、1颗后电源。取消SW_ACK、ACK网络及TP8，XIAO连线10根，D3不连接。旧五键板上的上方D2仍可用于验证功能，但下方D3键不再起作用；装配请使用四键版载板及外壳。

背面电源键与Type-C同高横排的载板仍使用D6，0.2.0安装包和固件可继续使用，无须为开关位置变化重新编译或烧录。
