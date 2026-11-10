# 收音机漏接提醒器软件验证版 0.4.0

原生 Java Android App + Seeed XIAO nRF52840 基础版 Arduino/Bluefruit 固件，严格沿用载板接线，不修改电子硬件或外壳。
可安装/烧录文件及中文说明在 `product/software-validation/v0.4.0`。这是首次功能联调包；编译和逻辑测试不代表真机验收。

## 运行范围

Android 最低26（8.0）、目标35（15），不含第三方分析SDK、不申请INTERNET、不查询号码或联系人。Android 16等新系统尚未测试。通知监听会收到系统交付的通知对象，但只处理默认拨号器/系统应用的 `CATEGORY_MISSED_CALL`，不读取标题、正文、号码，不存储通知内容。运行日志仅在内存保留30条状态事件；新增来电历史使用App私有SQLite数据库保存最近1000通来电，按日期分组，关闭进程/重启手机后仍可查看。只存来电开始时间、结果、证据来源和用于去重的系统记录ID，不存号码和联系人。卸载或清除应用数据会删除历史，系统备份已禁用。

App采用用户从前台启动的connectedDevice前台服务及低重要性常驻通知；已保存的挂件会断线重连，不重放旧提醒。开启监听后持久保存开关，并请求系统重建、任务移除检查、电话广播、重启/升级后的恢复。首次覆盖升级后需主动点一次“开始来电监听”启用。部分厂商仍需人工允许自启动与后台运行；被系统终止后不能保证无缝或固定时限恢复。

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

开发环境未连接手机或载板，0.4.0尚未进行安装运行、BLE收发、扬声器播放、按键唤醒或真实来电测试。用户已反馈旧0.3.x在小米MIX 2S / MIUI12.5清理后监听中断，不能将该反馈视作新版恢复已验证。桌面测试不验证Android后台调度、厂商拨号器通知、焊接/电气输出、音量、72小时续航或充电温升。

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

背面电源键与Type-C同高横排的载板仍使用D6，本版App配合0.2.0固件可继续使用，无须为开关位置变化重新编译或烧录。

## 来电历史与实时证据
首页“查看来电历史（按日期保存）”可离线查看最近1000通普通电话，分页100条；来电开始时间按当前时区分组，结果分为已接听、系统核验未接/拒接、通知推断疑似漏接、无法确认。响铃→空闲不能单独证明漏接，模拟按钮不写入真实历史。只存时间、结果、证据和去重ID，不存号码/联系人，卸载或清数据会删除历史。
数据库版本2；覆盖升级保留记录。没有实时证据的旧记录保持未知。每条显示“实时检测到响铃”或“系统记录补读”及首次保存时间。服务运行时，返回首页、刷新或启动会主动查询当前会话启动以来（含3秒匹配余量）的系统记录；不补录安装前/会话停机期间的旧电话，也不把补读算成实时成功。匹配窗口3秒，系统证据优先。提醒仅接收本次会话实际收到响铃、挂件连接后的新鲜电话；恢复后计数重置，不补响断档期间的电话。
电话监听器与冷启动广播不会同时向状态机输入同一事件流；冷广播额外抑制1.5秒内重复状态，且延迟超过10秒的冷启动事件不得伪装为实时事件。普通单路蜂窝电话为验证范围，双卡并发、呼叫等待、微信等网络通话不在已验证范围。

## 0.4.0 监听恢复
用户已明确旧版失败发生在划掉最近任务/一键清理后，且在MIUI设为无限制后仍会发生。恢复能力是此版新增代码，不是保证后台常驻。
- 在前台点开始、扫描或连接，持久启用监听开关。点“停止监听与自动恢复”时持久关闭并取消恢复检查，不会被下次电话或重启擅自开启。
- 正常运行返回START_STICKY，请求系统回收后重建服务；stopWithTask=false使标准任务移除不等同停止服务。
- onTaskRemoved或非主动的onDestroy安排一次setAndAllowWhileIdle非精确恢复检查，不申请精确闹钟，不轮询拉活；5秒是请求时间，不是恢复时限。若检查时服务仍在运行只记录结果；后台启动被拒绝后不循环重试。
- PHONE_STATE为Android允许静态注册的广播例外。进程未运行且开关开启时尝试前台服务冷启动，普通在运行的监听器独占事件处理，忽略广播附带的号码。Android 8–11允许的机会较多；Android12及以上仍受前台服务后台启动限制，不因为可接广播就必然可开服务。
- 收到解锁后的BOOT_COMPLETED、MY_PACKAGE_REPLACED时，仅为先前已启用者尝试恢复。主动打开App也会恢复已开启的监听。只在解锁后运行，不使用Direct Boot。
- 恢复后尝试重连已保存的挂件。蓝牙关闭/无适配器时保持电话服务并等待，不因连接失败反复崩溃；蓝牙权限撤回会暂停蓝牙连接。重连/进程重建重置计数，不能补发旧提醒。
- 首页显示自动恢复开关、最新检查/请求、最近自动启动成功来源与时间、最近失败原因。手动打开App后不会抹掉上次自动启动/失败证据。若系统根本不派发广播，App无法实时写入失败原因；通知权限关闭时也不能保证发出恢复提示。
- 增加小米自启动设置入口（miui.intent.action.OP_AUTO_START，无法解析时回退应用信息）；不修改系统开关，不要求悬浮窗/无障碍/Root，也不新增危险权限。

## 无法保证恢复的边界
| 情况 | 本版行为与边界 |
| --- | --- |
| 普通Home/锁屏 | 前台服务继续运行，仍需实测MIUI长时间调度与蓝牙时间同步 |
| 标准任务移除/普通系统回收 | 请求保留/重建或一次恢复检查，时机由系统决定 |
| MIUI一键清理同时禁止自启动、屏蔽广播或冻结进程 | 可能无法恢复；允许自启动、无限制和任务锁定可减少限制，不保证有效 |
| 系统应用信息中的强行停止、系统前台任务管理器停止 | 不绕过用户停止；需用户打开App，断档期间无法提醒 |
| App内主动停止监听 | 保持关闭，直到用户再次明确开启 |
| Android12及以上拒绝背景FGS启动 | 持久记录失败原因并尽可能通知用户，打开App后恢复；未加入伴生设备关联豁免 |
| 手机刚重启尚未解锁、没有首次启动授权、权限撤回 | 不承诺恢复，需解锁/授权/开启监听 |
| 蓝牙关闭、挂件关机/没电或不在范围 | 不能发出挂件提醒；恢复连接后不补响 |

## 检查与待测
79项桌面检查（25原逻辑、15历史状态、11实时/补读、18恢复策略、10SQLite存储迁移）和固件核心通过。APK保持同包名、同签名、最低Android8/API26、target35；不申请INTERNET。固件仍为0.2.0，无需重刷。
桌面策略测试不执行Android真实服务生命周期/广播投递/闹钟调度；必须在用户小米MIX 2S/MIUI12.5上完成覆盖升级、锁屏、划掉、一键清理、重启、主动停止与强停边界测试。Android12+启动拒绝和通知被禁的分支也需真机验证。未验证连续监听可靠率、功耗与BLE提醒端到端，不承诺清理后永远可用。

## Android恢复依据
- https://developer.android.com/reference/android/app/Service#START_STICKY
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
- https://developer.android.com/reference/android/telephony/TelephonyManager#ACTION_PHONE_STATE_CHANGED
- https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state
- https://dev.mi.com/docs/appsmarket/technical_docs/adaptation_FAQ/

## 跨品牌兼容性评估
0.4.0已按代码及官方资料编写《Android跨品牌适配与测试.md》和《Android兼容性测试记录.csv》（software/docs）。三星、Pixel、vivo/iQOO、OPPO、realme、一加、荣耀以及华为Android兼容系统均仅列为条件候选，尚无对应真机通过记录；原生鸿蒙不列入本APK支持范围。清单按安装、权限、真实来电、后台持续、清理恢复、挂件端到端分层验收，不能把补读当成后台实时成功。此次只补充评估与测试资料，APK和固件字节未变。
