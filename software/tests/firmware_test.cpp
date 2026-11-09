#include <cassert>
#include <cstdio>
#include "../firmware/RadioReminder/ReminderCore.h"
int main(){
 ReminderCore c;c.connection(true);assert(!c.command(2,1,0,0)||!c.alert);assert(!c.alert);
 assert(c.command(1,2,600,0));assert(c.command(2,3,0,1)&&c.alert);assert(c.note(1)==0);assert(c.gain(1)==0);assert(c.note(4000)==-1);assert(c.note(10001)==0);
 assert(!c.command(2,3,0,2));c.tick(90000);assert(!c.alert); // time lease fails closed
 c.command(1,4,1319,90001);c.command(2,5,0,90002);assert(c.alert);c.tick(150001);assert(!c.alert); // cross 22:00
 c.command(1,6,1320,150002);c.command(4,7,0,150003);assert(!c.preview);
 c.command(1,8,360,150004);c.command(4,9,0,150005);assert(c.preview);c.tick(151505);assert(!c.preview);
 c.command(2,10,0,151506);c.connection(false);assert(!c.alert&&!c.clock);c.connection(true);assert(c.lastSequence==0&&!c.clock);
 c.command(1,1,600,0xfffffff0);c.command(2,2,0,0xfffffff1);c.tick(10);assert(c.alert); // millis wrap
 assert(!c.command(6,3,3,11)&&c.volume==0);assert(c.command(6,3,2,11)&&c.volume==2);
 for(unsigned i=0;i<1600;i++){assert(c.gain(i)<=60);}
 c.connection(true);c.command(1,1,600,0);c.command(2,2,0,1);
 c.command(1,3,600,30000);c.command(1,4,601,60000);c.command(1,5,601,90000);c.tick(120000);assert(c.alert);c.tick(120001);assert(!c.alert);
 c.command(1,6,1320,120002);c.command(2,7,0,120003);assert(!c.alert);c.command(1,8,360,180000);c.tick(180001);assert(!c.alert); // no dawn replay
 ReminderCore d;d.connection(true);d.command(1,1,600,0);
 assert(!d.pressSound(1)&&d.sound==1&&d.preview); // idle press switches and previews
 d.command(2,2,0,10);assert(d.alert);assert(d.pressSound(20)&&!d.alert&&!d.preview&&d.sound==1); // active press only stops
 assert(d.lastSequence==2); // local button does not consume phone sequence
 d.command(2,3,0,30);assert(d.alert);d.command(1,4,1320,40);
 assert(!d.pressSound(50)&&d.sound==0&&!d.preview); // night still changes selection without sound
 d.connection(false);assert(!d.pressSound(60)&&d.sound==1&&!d.preview); // no clock fails silent
 puts("Firmware core: protocol, time lease, night boundary, reconnect, volume and timer wrap passed");
}
