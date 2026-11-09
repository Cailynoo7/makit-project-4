#pragma once
#include <stdint.h>
struct ReminderCore {
 bool connected=false,clock=false,alert=false,preview=false;
 uint8_t sound=0,volume=0;uint16_t baseMinute=0;
 uint32_t syncAt=0,alertAt=0,playAt=0,lastSequence=0;
 static uint32_t elapsed(uint32_t now,uint32_t then){return now-then;}
 bool timeValid(uint32_t now)const{return connected&&clock&&elapsed(now,syncAt)<90000;}
 uint16_t minute(uint32_t now)const{return (baseMinute+elapsed(now,syncAt)/60000)%1440;}
 bool quiet(uint32_t now)const{uint16_t m=minute(now);return m>=1320||m<360;}
 bool allowed(uint32_t now)const{return timeValid(now)&&!quiet(now);}
 void stop(){alert=false;preview=false;}
 // Returns true only when a press stops an active reminder; it does not mean the phone was viewed.
 bool pressSound(uint32_t now){
  if(alert){stop();return true;}
  sound=1-sound;preview=allowed(now);playAt=now;return false;
 }
 void connection(bool c){connected=c;clock=false;lastSequence=0;stop();}
 bool command(uint8_t op,uint32_t seq,uint32_t arg,uint32_t now){
  if(!connected||seq==0||seq<=lastSequence)return false;
  // Validate before consuming sequence; unknown commands have no effect.
  if(op<1||op>6||(op==1&&arg>=1440)||(op==5&&arg>1)||(op==6&&arg>2))return false;
  lastSequence=seq;
  if(op==1){baseMinute=arg;syncAt=now;clock=true;if(quiet(now))stop();}
  if(op==2&&allowed(now)){if(!alert){alert=true;alertAt=now;playAt=now;preview=false;}}
  if(op==3)stop();
  if(op==4&&allowed(now)&&!alert){preview=true;playAt=now;}
  if(op==5)sound=arg;
  if(op==6)volume=arg;
  return true;
 }
 void tick(uint32_t now){if(!allowed(now)){stop();return;}if(alert&&elapsed(now,alertAt)>=120000)stop();if(preview&&elapsed(now,playAt)>=1500)preview=false;}
 // Full alert repeats every 10 seconds. Return note index or -1 for silence.
 int note(uint32_t now)const{
  if(!allowed(now)||(!alert&&!preview))return -1;
  uint32_t phase=elapsed(now,alert?alertAt:playAt)%(alert?10000:100000);
  uint16_t span=sound?230:330, length=sound?140:250;
  if(phase>=4u*span||phase%span>=length)return -1;
  return phase/span;
 }
 uint8_t gain(uint32_t now)const{
  static const uint8_t amplitudes[]={18,36,60};
  uint16_t span=sound?230:330,length=sound?140:250;
  uint32_t phase=elapsed(now,alert?alertAt:playAt)%(alert?10000:100000),p=phase%span;
  if(note(now)<0)return 0;
  uint16_t envelope=p<35?p:(p>length-35?length-p:35);
  return (uint16_t)amplitudes[volume]*envelope/35;
 }
};
