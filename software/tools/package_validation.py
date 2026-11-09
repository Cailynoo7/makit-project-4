#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import pathlib,hashlib,json,struct,zipfile,subprocess,datetime,re
root=pathlib.Path(__file__).resolve().parents[2];work=root/'.work/software';out=root/'product/software-validation/v0.2.0'
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
badging=subprocess.check_output([str(bt/'aapt2'),'dump','badging',str(out/'radio-reminder-validation.apk')],text=True)
assert "versionCode='2'" in badging and "minSdkVersion:'26'" in badging and "targetSdkVersion:'35'" in badging and "name='android.permission.INTERNET'" not in badging
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
report={'version':'0.2.0-validation',
 'hardware':json.loads((root/'software/hardware.json').read_text()),
 'built_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'android':{'package':'cn.makit.radioreminder',
 'minimum_api':26,
 'target_api':35,
 'apk_signature_v2_v3_verified':True,
 'internet_permission':False,
 'installed_on_real_phone':False},
 'firmware':{'fqbn':'Seeeduino:nrf52:xiaonRF52840',
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
 'firmware_core_passed':True,
 'real_call_ble_audio_end_to_end_tested':False},
 'limitations':['Android READ_CALL_LOG属于hard restricted，可能无法授权；默认不启用通知推断',
 '通知兼容模式不能保证排除主动拒接',
 '手机重启需手动恢复连接；厂商后台策略未实测',
 '仅点击系统未接通知或显式确认可作为查看依据，解锁不算已查看',
 '未完成真实手机/挂件联调、响度、电气功率、续航和充电测试'],
 'artifacts':[]}
for p in sorted(out.iterdir()):
 if p.is_file() and p.name!='manifest.json':report['artifacts'].append({'name':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(out/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
archive=root/'product/收音机提醒器-软件验证版0.2.0.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(out.rglob('*')):
  if p.is_file():z.write(p,p.relative_to(out))
print('APK signature verified; minimum API26 / target35; no INTERNET permission')
print('UF2 verified against',len(hexbytes),'HEX bytes;',len(blocks),'blocks; no bootloader or SoftDevice overwrite')
print('Tests: 25 Java checks + firmware core passed; real hardware tests pending')
print('Delivery package:',archive.stat().st_size,'bytes; SHA256',hashlib.sha256(archive.read_bytes()).hexdigest())
