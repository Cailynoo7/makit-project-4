#!/usr/bin/env python3
import pathlib,os,subprocess,shutil
root=pathlib.Path(__file__).resolve().parents[2];work=root/'.work/software'
# The vendor recipe invokes `python`; keep the alias project-local.
binpath=work/'bin';binpath.mkdir(exist_ok=True,parents=True)
python=binpath/'python'
if python.is_symlink():python.unlink()
python.write_text('#!/bin/sh\nexec /usr/bin/python3 "$@"\n');python.chmod(0o755)
env=os.environ.copy();env['PATH']=str(binpath)+os.pathsep+env['PATH']
cli=work/'toolchains/arduino-cli';config=work/'arduino-cli.yaml';build=work/'firmware-build'
subprocess.run([str(cli),'compile','--config-file',str(config),'--fqbn','Seeeduino:nrf52:xiaonRF52840','--build-path',str(build),str(root/'software/firmware/RadioReminder')],cwd=root,env=env,check=True)
core=work/'arduino-data/packages/Seeeduino/hardware/nrf52/1.1.13'
subprocess.run(['/usr/bin/python3',str(core/'tools/uf2conv/uf2conv.py'),'-f','0xADA52840','-c','-o',str(build/'RadioReminder.uf2'),str(build/'RadioReminder.ino.hex')],check=True)
out=root/'product/software-validation/v0.2.0';out.mkdir(exist_ok=True,parents=True)
for src,dst in [('RadioReminder.uf2','radio-reminder-xiao-nrf52840.uf2'),('RadioReminder.ino.hex','radio-reminder-xiao-nrf52840.hex'),('RadioReminder.ino.zip','radio-reminder-serial-dfu.zip')]:shutil.copy2(build/src,out/dst)
