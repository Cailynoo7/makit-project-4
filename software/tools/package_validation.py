#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import pathlib,hashlib,json,struct,zipfile,subprocess,datetime,re
root=pathlib.Path(__file__).resolve().parents[2];work=root/'.work/software'
version=json.loads((root/'software/version.json').read_text());out=root/'product/software-validation'/version['directory']
# Verify UF2 family, addresses and exact payload against the compiler's Intel HEX.
hexbytes={};base=0
for line in (out/'radio-reminder-xiao-nrf52840.hex').read_text().splitlines():
 b=bytes.fromhex(line[1:]);assert sum(b)%256==0
 n=b[0];address=int.from_bytes(b[1:3],'big');kind=b[3];data=b[4:4+n]
 if kind==4:base=int.from_bytes(data,'big')<<16
 if kind==2:base=int.from_bytes(data,'big')<<4
 if kind==0:
  for i,v in enumerate(data):hexbytes[base+address+i]=v
uf=(out/'radio-reminder-xiao-nrf52840.uf2').read_bytes();assert len(uf)%512==0
actual={};blocks=set()
for offset in range(0,len(uf),512):
 block=uf[offset:offset+512];magic1,magic2,flags,address,size,num,total,family=struct.unpack_from('<8I',block)
 assert (magic1,magic2)==(0x0A324655,0x9E5D5157) and struct.unpack_from('<I',block,508)[0]==0x0AB16F30
 assert flags&0x2000 and family==0xADA52840 and size<=476 and address>=0x27000 and address+size<=0xED000
 assert total==len(uf)//512 and num not in blocks;blocks.add(num)
 for i,v in enumerate(block[32:32+size]):actual[address+i]=v
assert all(actual.get(a)==v for a,v in hexbytes.items())
with zipfile.ZipFile(out/'radio-reminder-serial-dfu.zip') as z:assert z.testzip() is None
jdk=next((work/'toolchains').glob('*/Contents/Home'))
env=__import__('os').environ.copy();env['JAVA_HOME']=str(jdk);env['PATH']=str(jdk/'bin')+':'+env['PATH']
bt=work/'toolchains/android/android-15'
sig=subprocess.check_output([str(bt/'apksigner'),'verify','--verbose','--print-certs',str(out/'radio-reminder-validation.apk')],env=env,text=True)
assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in sig
assert 'Verified using v3 scheme (APK Signature Scheme v3): true' in sig
old_sig=subprocess.check_output([str(bt/'apksigner'),'verify','--print-certs',str(root/'product/software-validation/v0.2.0/radio-reminder-validation.apk')],env=env,text=True)
cert=lambda output: re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',output)[1]
assert cert(sig)==cert(old_sig), 'Signing certificate changed; cannot update existing installation'
badging=subprocess.check_output([str(bt/'aapt2'),'dump','badging',str(out/'radio-reminder-validation.apk')],text=True)
assert ("versionCode='%s'" % version['code']) in badging and "minSdkVersion:'26'" in badging and "targetSdkVersion:'35'" in badging and "name='android.permission.INTERNET'" not in badging
old_badging=subprocess.check_output([str(bt/'aapt2'),'dump','badging',str(root/'product/software-validation/v0.3.1/radio-reminder-validation.apk')],text=True)
permissions=lambda text:set(re.findall(r"uses-permission: name='([^']+)'",text))
assert permissions(badging)==permissions(old_badging), 'Permission set changed'
xmltree=subprocess.check_output([str(bt/'aapt2'),'dump','xmltree','--file','AndroidManifest.xml',str(out/'radio-reminder-validation.apk')],text=True)
for component in ['PhoneStateReceiver','RecoveryReceiver','BootNotice','android.intent.action.PHONE_STATE','android.intent.action.MY_PACKAGE_REPLACED','stopWithTask']:
 assert component in xmltree, component
# Copy complete test evidence, with portable paths for shared reports.
tests=json.loads((work/'test-results.json').read_text());assert all(r['exit_code']==0 for r in tests['results'])
for r in tests['results']:r['command']=[x.replace(str(root),'${PROJECT}') for x in r['command']]
(out/'test-results.json').write_text(json.dumps(tests,ensure_ascii=False,indent=2)+'\n')
(out/'apk-verification.txt').write_text(sig+'\n'+badging)
source_zip=out/'软件源代码.zip'
with zipfile.ZipFile(source_zip,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted((root/'software').rglob('*')):
  if p.is_file() and '__pycache__' not in p.parts and 'releases' not in p.parts:z.write(p,p.relative_to(root))
firmware_log=(work/'firmware-build-final.log').read_text();mem=re.search(r'Sketch uses (\d+) bytes',firmware_log);ram=re.search(r'Global variables use (\d+) bytes',firmware_log)
report={'version':version['name'],
 'hardware':json.loads((root/'software/hardware.json').read_text()),
 'built_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'android':{'package':'cn.makit.radioreminder',
 'minimum_api':26,
 'target_api':35,
 'apk_signature_v2_v3_verified':True,
 'internet_permission':False,
 'installed_on_real_phone':False,
 'same_signing_certificate_as_v0_2_0':True,
 'persistent_call_history':True,
 'live_vs_backfill_evidence':True,
 'recovery_paths':['START_STICKY','task_removed_inexact_alarm','PHONE_STATE_broadcast','BOOT_COMPLETED','MY_PACKAGE_REPLACED','user_reopen'],
 'recovery_requires_prior_user_enable':True,
 'new_dangerous_permissions':False,
 'permission_set_matches_v0_3_1':True,
 'recovery_receivers_present_in_apk':True,
 'database_version':2,
 'history_max_records':1000},
 'firmware':{'version':version['firmware_version'],'unchanged_from_v0_2_0':True,'fqbn':'Seeeduino:nrf52:xiaonRF52840',
 'core':'1.1.13',
 'softdevice_required':'S140 7.3.0',
 'application_start':'0x27000',
 'uf2_family':'0xADA52840',
 'uf2_matches_compiled_hex':True,
 'flash_bytes':int(mem[1]),
 'ram_bytes':int(ram[1]),
 'flashed_on_real_hardware':False},
 'tests':{'java_checks':25,
 'java_passed':True,
 'history_state_checks':15,
 'history_storage_checks':10,
 'call_observation_checks':11,
 'recovery_policy_checks':18,
 'miui_clear_recents_recovery_real_device_tested':False,
 'boot_recovery_real_device_tested':False,
 'android12_background_denial_real_device_tested':False,
 'history_persistence_desktop_sqlite_tested':True,
 'android_database_reboot_tested':False,
 'call_history_ui_real_device_tested':False,
 'firmware_core_passed':True,
 'real_call_ble_audio_end_to_end_tested':False},
 'limitations':['系统记录补读不代表后台实时监听成功，不用于触发挂件补响',
 '系统强行停止、厂商禁止自启动/广播或后台冻结时，无法保证恢复',
 'Android12及以上可能拒绝电话广播或非精确闹钟启动前台服务；失败时记录原因并尽可能通知用户',
 '恢复会建立新监听会话与蓝牙连接，期间断档不补响，非精确恢复检查没有固定时限',
 'Android READ_CALL_LOG属于hard restricted，可能无法授权；默认不启用通知推断',
 '通知兼容模式不能保证排除主动拒接',
 '手机重启后已开启监听者尝试恢复；首次解锁、权限及厂商策略仍可能阻止，尚未真机验证',
 '仅点击系统未接通知或显式确认可作为查看依据，解锁不算已查看',
 '未完成真实手机/挂件联调、响度、电气功率、续航和充电测试'],
 'compatibility_review':{'scope':'code_and_official_sources_only','cross_brand_real_device_verified':False,'guide':'Android跨品牌适配与测试.md','test_record_template':'Android兼容性测试记录.csv'},
 'artifacts':[]}
for p in sorted(out.iterdir()):
 if p.is_file() and p.name!='manifest.json':report['artifacts'].append({'name':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(out/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
archive=root/('product/收音机提醒器-软件验证版'+version['directory'][1:]+'.zip')
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(out.rglob('*')):
  if p.is_file():z.write(p,p.relative_to(out))
print('APK signature verified; minimum API26 / target35; no INTERNET permission')
print('UF2 verified against',len(hexbytes),'HEX bytes;',len(blocks),'blocks; no bootloader or SoftDevice overwrite')
print('Tests: 25 engine + 15 history state + 10 SQLite storage + 11 observation + 18 recovery policy checks + firmware core passed; Android device tests pending')
print('Delivery package:',archive.stat().st_size,'bytes; SHA256',hashlib.sha256(archive.read_bytes()).hexdigest())
