#!/usr/bin/env python3
import pathlib,subprocess,json
root=pathlib.Path(__file__).resolve().parents[2]
work=root/'.work/software';jdk=next((work/'toolchains').glob('*/Contents/Home'))/'bin'
out=work/'java-tests';out.mkdir(exist_ok=True,parents=True)
commands=[
 [str(jdk/'javac'),'-d',str(out),*[str(root/f'software/android/app/src/main/java/cn/makit/radioreminder/{n}.java') for n in ['MissedEngine','Protocol','IncomingEpisode','CallHistoryState','CallObservation','RecoveryPolicy']],str(root/'software/tests/EngineTest.java'),str(root/'software/tests/CallHistoryStateTest.java'),str(root/'software/tests/CallObservationTest.java'),str(root/'software/tests/RecoveryPolicyTest.java')],
 [str(jdk/'java'),'-cp',str(out),'EngineTest'],
 [str(jdk/'java'),'-cp',str(out),'CallHistoryStateTest'],
 [str(jdk/'java'),'-cp',str(out),'CallObservationTest'],
 [str(jdk/'java'),'-cp',str(out),'RecoveryPolicyTest'],
 ['/usr/bin/python3',str(root/'software/tests/history_storage_test.py')],
 ['clang++','-std=c++17','-Wall','-Wextra','-Werror',str(root/'software/tests/firmware_test.cpp'),'-o',str(work/'firmware_test')],
 [str(work/'firmware_test')]
]
results=[]
for cmd in commands:
 r=subprocess.run(cmd,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 print(r.stdout,end='');results.append({'command':cmd,'exit_code':r.returncode,'output':r.stdout})
 if r.returncode:raise SystemExit(r.returncode)
(work/'test-results.json').write_text(json.dumps({'results':results},ensure_ascii=False,indent=2)+'\n')
