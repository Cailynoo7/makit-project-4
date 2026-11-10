"""Exercise the exact Android SQLite schema/prune SQL across database reopen."""
from pathlib import Path
import sqlite3, tempfile, re
root=Path(__file__).resolve().parents[2]
source=(root/'software/android/app/src/main/java/cn/makit/radioreminder/CallHistoryDb.java').read_text()
sql={name:re.search(r'public static final String '+name+r'="([^"]+)"',source)[1] for name in ('CREATE','PRUNE')}
checks=0
def check(value):
 global checks
 assert value
 checks+=1
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'history.db'
 with sqlite3.connect(p) as db:
  db.execute(sql['CREATE'])
  db.execute('INSERT INTO calls(started_at,result,evidence) VALUES(?,?,?)',(1735689599000,'answered','手机来电状态'))
  db.execute('INSERT INTO calls(started_at,result,evidence,system_id) VALUES(?,?,?,?)',(1735689601000,'missed','系统核验','42'))
 # Close and reopen: this is persistent storage, not process-static data.
 with sqlite3.connect(p) as db:
  check(db.execute('SELECT result FROM calls ORDER BY started_at').fetchall()==[('answered',),('missed',)])
  check([x[1] for x in db.execute('PRAGMA table_info(calls)')]==['_id','started_at','result','evidence','system_id','live_seen','first_saved_at'])
  try:
   db.execute('INSERT INTO calls(started_at,result,evidence,system_id) VALUES(?,?,?,?)',(1735689601000,'missed','系统核验','42'))
   raise AssertionError('duplicate system call accepted')
  except sqlite3.IntegrityError: check(True)
  db.executemany('INSERT INTO calls(started_at,result,evidence) VALUES(?,?,?)',((1735689700000+i,'unknown','测试') for i in range(1100)))
  db.execute(sql['PRUNE'])
  check(db.execute('SELECT COUNT(*) FROM calls').fetchone()[0]==1000)
  check(db.execute('SELECT MIN(started_at),MAX(started_at) FROM calls').fetchone()==(1735689700100,1735689701099))
  check(len(db.execute('SELECT * FROM calls ORDER BY started_at DESC,_id DESC LIMIT 100,100').fetchall())==100)
  db.execute('DELETE FROM calls')
 with sqlite3.connect(p) as db: check(db.execute('SELECT COUNT(*) FROM calls').fetchone()[0]==0)
# Upgrade the old on-device schema using the migration statements from Android source.
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'legacy.db'
 with sqlite3.connect(p) as db:
  db.execute("CREATE TABLE calls (_id INTEGER PRIMARY KEY AUTOINCREMENT, started_at INTEGER NOT NULL, result TEXT NOT NULL, evidence TEXT NOT NULL, system_id TEXT UNIQUE)")
  db.execute("INSERT INTO calls(started_at,result,evidence) VALUES(1000,'answered','old')")
  for statement in re.findall(r'db.execSQL\("(ALTER TABLE calls [^"]+)"\)',source):db.execute(statement)
  check(db.execute('SELECT result,live_seen,first_saved_at FROM calls').fetchone()==('answered',None,None))
  db.execute("INSERT INTO calls(started_at,result,evidence,live_seen,first_saved_at) VALUES(2000,'unknown','phone',1,2001)")
  db.execute("UPDATE calls SET result='missed',system_id='43' WHERE started_at=2000")
  check(db.execute('SELECT live_seen,first_saved_at FROM calls WHERE system_id=?',('43',)).fetchone()==(1,2001))
  db.execute("INSERT INTO calls(started_at,result,evidence,system_id,live_seen,first_saved_at) VALUES(3000,'missed','system','44',0,9000)")
 with sqlite3.connect(p) as db:
  check(db.execute("SELECT live_seen,first_saved_at FROM calls WHERE system_id='44'").fetchone()==(0,9000))
print('HistoryStorageTest:',checks,'checks passed (reopen, privacy, dedupe, retention, paging, deletion)')
