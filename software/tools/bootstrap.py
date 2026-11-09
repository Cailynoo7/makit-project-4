#!/usr/bin/env python3
"""Install pinned build tools locally. No system configuration is changed."""
import pathlib,json,hashlib,subprocess,tarfile,zipfile
root=pathlib.Path(__file__).resolve().parents[2];work=root/'.work/software';tools=work/'toolchains';downloads=work/'downloads'
tools.mkdir(parents=True,exist_ok=True);downloads.mkdir(exist_ok=True)
lock=json.loads((root/'software/tools/toolchain-lock.json').read_text())
for name,item in lock['downloads'].items():
 p=downloads/name
 if not p.exists() or hashlib.sha256(p.read_bytes()).hexdigest()!=item['sha256']:
  subprocess.run(['curl','-LsSf','--retry','2','--max-time','600','-o',str(p),item['url']],check=True)
 if hashlib.sha256(p.read_bytes()).hexdigest()!=item['sha256']:raise SystemExit('Checksum mismatch: '+name)
 if name.endswith('.tar.gz'):
  # Archives are pinned by hash and come from the named upstream vendor.
  with tarfile.open(p) as f:f.extractall(tools)
 else:
  with zipfile.ZipFile(p) as f:f.extractall(tools/'android')
  # zipfile does not restore executable bits on macOS.
  for n in ['aapt2','zipalign','d8','apksigner']:(tools/'android/android-15'/n).chmod(0o755)
config=work/'arduino-cli.yaml'
config.write_text('board_manager:\n  additional_urls:\n    - https://files.seeedstudio.com/arduino/package_seeeduino_boards_index.json\ndirectories:\n  data: '+str(work/'arduino-data')+'\n  downloads: '+str(work/'arduino-downloads')+'\n  user: '+str(work/'arduino-user')+'\n')
for args in [['core','update-index'],['core','install','Seeeduino:nrf52@1.1.13']]:subprocess.run([str(tools/'arduino-cli'),*args,'--config-file',str(config)],cwd=root,check=True)
print('Local toolchains ready. Run build_android.py, build_firmware.py and run_tests.py.')
