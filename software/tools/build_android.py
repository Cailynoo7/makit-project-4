#!/usr/bin/env python3
"""Build a native Java APK with the official Android SDK tools, no Gradle dependencies."""
import os,pathlib,subprocess,shutil,hashlib,json
ROOT=pathlib.Path(__file__).resolve().parents[2]
VERSION=json.loads((ROOT/'software/version.json').read_text())
WORK=ROOT/'.work/software'; TOOLS=WORK/'toolchains'
jdks=list(TOOLS.glob('*/zulu-*.jdk/Contents/Home'))+list(TOOLS.glob('*/Contents/Home'))
java=pathlib.Path(os.environ['RADIO_JAVA_HOME']) if 'RADIO_JAVA_HOME' in os.environ else jdks[0]
sdk=TOOLS/'android';bt=sdk/'android-15';android=sdk/'android-35/android.jar'
build=WORK/'android-build';build.mkdir(parents=True,exist_ok=True)
classes=build/'classes';dex=build/'dex';generated=build/'generated'
for d in (classes,dex,generated):
 if d.exists():shutil.rmtree(d)
 d.mkdir()
env=os.environ.copy();env['JAVA_HOME']=str(java);env['PATH']=str(java/'bin')+os.pathsep+env['PATH']
def run(*args):subprocess.run([str(a) for a in args],cwd=ROOT,env=env,check=True)
src=ROOT/'software/android/app/src/main'
run(bt/'aapt2','link','-I',android,'--manifest',src/'AndroidManifest.xml','--java',generated,'--version-code',str(VERSION['code']),'--version-name',VERSION['name'],'-o',build/'resources.apk')
java_files=list((src/'java').rglob('*.java'))+list(generated.rglob('*.java'))
run(java/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-classpath',android,'-d',classes,*java_files)
run(bt/'d8','--lib',android,'--min-api','26','--output',dex,*classes.rglob('*.class'))
shutil.copy2(build/'resources.apk',build/'unsigned.apk')
run(java/'bin/jar','uf',build/'unsigned.apk','-C',dex,'classes.dex')
run(bt/'zipalign','-f','4',build/'unsigned.apk',build/'aligned.apk')
key=WORK/'validation-signing.jks'
if not key.exists():run(java/'bin/keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','validation','-dname','CN=Makit Validation ONLY','-keyalg','RSA','-keysize','2048','-validity','3650')
out=ROOT/'product/software-validation'/VERSION['directory']/'radio-reminder-validation.apk'
out.parent.mkdir(parents=True,exist_ok=True)
run(bt/'apksigner','sign','--v4-signing-enabled','false','--ks',key,'--ks-key-alias','validation','--ks-pass','pass:android','--key-pass','pass:android','--out',out,build/'aligned.apk')
run(bt/'apksigner','verify','--verbose','--print-certs',out)
run(bt/'aapt2','dump','badging',out)
print('APK SHA256',hashlib.sha256(out.read_bytes()).hexdigest())
