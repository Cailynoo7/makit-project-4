#include <Arduino.h>
#include <bluefruit.h>
#include <Adafruit_LittleFS.h>
#include <InternalFileSystem.h>
#include <math.h>
#include <queue.h>
#include <utility/bonding.h>
#include "ReminderCore.h"
using namespace Adafruit_LittleFS_Namespace;
// UUID arrays are little endian as required by the Bluefruit API.
const uint8_t SERVICE_UUID[]={0x01,0x4f,0x49,0x44,0x41,0x52,0x30,0x9d,0x8a,0x4b,0x6f,0x9d,0x00,0x00,0x71,0x43};
const uint8_t COMMAND_UUID[]={0x01,0x4f,0x49,0x44,0x41,0x52,0x30,0x9d,0x8a,0x4b,0x6f,0x9d,0x01,0x00,0x71,0x43};
const uint8_t STATUS_UUID[]={0x01,0x4f,0x49,0x44,0x41,0x52,0x30,0x9d,0x8a,0x4b,0x6f,0x9d,0x02,0x00,0x71,0x43};
BLEService service(SERVICE_UUID);BLECharacteristic commandChar(COMMAND_UUID),statusChar(STATUS_UUID);
ReminderCore core;
constexpr uint8_t AUDIO=0,AMP=1,SOUND_KEY=2,UP_KEY=4,DOWN_KEY=5,POWER_KEY=6;
uint16_t wave[64];int previousNote=-1;uint8_t previousGain=255;bool ampOn=false,shuttingDown=false,dirty=false,lowBattery=false;
uint32_t statusAt=0,saveAt=0,batteryAt=0,bootAt=0;volatile uint16_t connectionHandle=BLE_CONN_HANDLE_INVALID;
struct LinkEvent {uint8_t kind;uint16_t conn;uint8_t data[10];};
QueueHandle_t linkEvents;
struct Button {uint8_t pin;bool raw=false,down=false,longSent=false;uint32_t changed=0,pressed=0;explicit Button(uint8_t p):pin(p){} };
Button buttons[]={Button(SOUND_KEY),Button(UP_KEY),Button(DOWN_KEY),Button(POWER_KEY)};
static_assert(sizeof(buttons)/sizeof(buttons[0])==4,"Four physical keys required");
void put32(uint8_t* p,uint32_t v){for(int i=0;i<4;i++)p[i]=(v>>(8*i))&255;}
uint32_t get32(const uint8_t* p){uint32_t v=0;for(int i=0;i<4;i++)v|=(uint32_t)p[i]<<(8*i);return v;}
void notifyStatus(uint8_t event=0x80){
 uint32_t now=millis();uint32_t bits=(core.alert?1:0)|(core.timeValid(now)?2:0)|(core.timeValid(now)&&core.quiet(now)?4:0)|(core.connected?8:0)|(core.sound<<4)|(core.volume<<5)|(lowBattery?256:0);
 uint8_t b[10]={1,event};put32(b+2,core.lastSequence);put32(b+6,bits);statusChar.write(b,10);if(core.connected)statusChar.notify(b,10);statusAt=now;
}
void stopAudio(){if(ampOn){digitalWrite(AMP,LOW);NRF_PWM3->TASKS_STOP=1;NRF_PWM3->ENABLE=0;digitalWrite(AUDIO,LOW);ampOn=false;}previousNote=-1;previousGain=255;}
void updateAudio(){
 uint32_t now=millis();int note=core.note(now);uint8_t gain=core.gain(now);
 if(note<0||!gain||shuttingDown){stopAudio();return;}
 uint16_t count=(core.sound||!(note&1))?18:24; // 889 Hz / 667 Hz at 16 ksample/s.
 if(note==previousNote&&gain==previousGain)return;
 if(!ampOn||note!=previousNote){
  digitalWrite(AMP,LOW);NRF_PWM3->TASKS_STOP=1;NRF_PWM3->ENABLE=0;
  NRF_PWM3->PSEL.OUT[0]=g_ADigitalPinMap[AUDIO];for(int i=1;i<4;i++)NRF_PWM3->PSEL.OUT[i]=0xffffffff;
  NRF_PWM3->MODE=0;NRF_PWM3->PRESCALER=0;NRF_PWM3->COUNTERTOP=250;NRF_PWM3->DECODER=0;
  NRF_PWM3->SEQ[0].PTR=(uint32_t)wave;NRF_PWM3->SEQ[0].CNT=count;NRF_PWM3->SEQ[0].REFRESH=3;NRF_PWM3->SEQ[0].ENDDELAY=0;
  NRF_PWM3->LOOP=1;NRF_PWM3->SHORTS=PWM_SHORTS_LOOPSDONE_SEQSTART0_Msk;
 }
 for(uint16_t i=0;i<count;i++)wave[i]=(uint16_t)(125+gain*sinf(2.0f*PI*i/count));
 if(!ampOn||note!=previousNote){NRF_PWM3->ENABLE=1;NRF_PWM3->TASKS_SEQSTART[0]=1;digitalWrite(AMP,HIGH);ampOn=true;}
 previousNote=note;previousGain=gain;
}
void saveSettings(){File f(InternalFS);if(f.open("/settings",FILE_O_WRITE)){uint8_t data[]={0x52,1,core.sound,core.volume};f.write(data,4);f.close();}dirty=false;}
void loadSettings(){File f(InternalFS);if(f.open("/settings",FILE_O_READ)){uint8_t data[4];if(f.read(data,4)==4&&data[0]==0x52&&data[1]==1&&data[2]<2&&data[3]<3){core.sound=data[2];core.volume=data[3];}f.close();}}
void markSettings(){dirty=true;saveAt=millis();notifyStatus();}
void commandWritten(uint16_t conn,BLECharacteristic*,uint8_t* data,uint16_t len){
 if(len!=10||data[0]!=1||conn!=connectionHandle)return;
 BLEConnection* c=Bluefruit.Connection(conn);if(!c||!c->secured())return;
 LinkEvent event={};event.kind=2;event.conn=conn;memcpy(event.data,data,10);
 if(xQueueSend(linkEvents,&event,0)!=pdTRUE)Bluefruit.disconnect(conn);
}
void connected(uint16_t conn){connectionHandle=conn;LinkEvent e={};e.kind=1;e.conn=conn;xQueueSend(linkEvents,&e,0);}
void disconnected(uint16_t conn,uint8_t){connectionHandle=BLE_CONN_HANDLE_INVALID;LinkEvent e={};e.kind=0;e.conn=conn;xQueueSend(linkEvents,&e,0);}
void linkTick(){
 LinkEvent e;while(xQueueReceive(linkEvents,&e,0)==pdTRUE){
  if(e.kind<2){
   if(e.kind==1){BLEConnection* c=Bluefruit.Connection(e.conn);if(!c||e.conn!=connectionHandle)continue;bond_keys_t keys;ble_gap_addr_t addr=c->getPeerAddr();
    if(millis()-bootAt>=60000&&!bond_load_keys(BLE_GAP_ROLE_PERIPH,&addr,&keys)){Bluefruit.disconnect(e.conn);continue;}}
   core.connection(e.kind==1);if(e.kind==0)stopAudio();else notifyStatus();}
  else if(e.conn==connectionHandle){uint8_t op=e.data[1];if(core.command(op,get32(e.data+2),get32(e.data+6),millis())){if(op==5||op==6)markSettings();else notifyStatus();}}
 }
 // Connection loss also fails closed if a callback queue ever overflows.
 if(core.connected&&connectionHandle==BLE_CONN_HANDLE_INVALID){core.connection(false);stopAudio();}
}
void goOff(){
 core.stop();stopAudio();if(dirty)saveSettings();
 digitalWrite(LED_RED,HIGH);digitalWrite(LED_GREEN,HIGH);digitalWrite(LED_BLUE,HIGH);
 nrf_gpio_cfg_sense_input(g_ADigitalPinMap[POWER_KEY],NRF_GPIO_PIN_PULLUP,NRF_GPIO_PIN_SENSE_LOW);
 sd_power_system_off();
}
void buttonsTick(){
 uint32_t now=millis();for(int i=0;i<4;i++){
  Button &b=buttons[i];bool raw=digitalRead(b.pin)==LOW;
  if(raw!=b.raw){b.raw=raw;b.changed=now;}
  if(raw!=b.down&&now-b.changed>=30){b.down=raw;if(raw){b.pressed=now;b.longSent=false;}else if(!b.longSent){
   if(i==0){if(core.pressSound(now)){stopAudio();notifyStatus(0x81);}else markSettings();}
   if(i==1&&core.volume<2){core.volume++;markSettings();}
   if(i==2&&core.volume>0){core.volume--;markSettings();}
  }}
  if(b.down&&!b.longSent&&now-b.pressed>=3000&&i==0){b.longSent=true;bootAt=now;}
  if(b.down&&!b.longSent&&now-b.pressed>=2000&&i==3){b.longSent=true;shuttingDown=true;core.stop();stopAudio();Bluefruit.Advertising.restartOnDisconnect(false);Bluefruit.Advertising.stop();if(core.connected)Bluefruit.disconnect(connectionHandle);}
 }
 if(shuttingDown&&!buttons[3].down)goOff();
}
void batteryTick(){
 if(millis()-batteryAt<60000)return;batteryAt=millis();
 // XIAO basic v1.2: P0.14 divider control; P0.31 ADC; ratio (1M+510k)/510k.
 pinMode(VBAT_ENABLE,OUTPUT);digitalWrite(VBAT_ENABLE,LOW);delay(2);
 analogReference(AR_INTERNAL_3_0);analogReadResolution(12);
 float volts=analogRead(PIN_VBAT)*3.0f/4095.0f*(1510.0f/510.0f);
 digitalWrite(VBAT_ENABLE,HIGH);lowBattery=volts>1.0f&&volts<3.45f;notifyStatus();
}
void setup(){
 pinMode(AUDIO,OUTPUT);digitalWrite(AUDIO,LOW);pinMode(AMP,OUTPUT);digitalWrite(AMP,LOW);
 for(auto &b:buttons)pinMode(b.pin,INPUT_PULLUP);
 // P0.13 selects low charging current; use board variant's actual Arduino index.
 pinMode(PIN_CHARGING_CURRENT,OUTPUT);digitalWrite(PIN_CHARGING_CURRENT,HIGH);
 pinMode(VBAT_ENABLE,OUTPUT);digitalWrite(VBAT_ENABLE,HIGH);
 const uint8_t leds[]={LED_RED,LED_GREEN,LED_BLUE};for(uint8_t led:leds){pinMode(led,OUTPUT);digitalWrite(led,HIGH);}
 bool wake=(NRF_POWER->RESETREAS&POWER_RESETREAS_OFF_Msk)!=0;NRF_POWER->RESETREAS=0xffffffff;
 if(wake){uint32_t start=millis();while(digitalRead(POWER_KEY)==LOW&&millis()-start<2000)delay(5);if(millis()-start<2000){nrf_gpio_cfg_sense_input(g_ADigitalPinMap[POWER_KEY],NRF_GPIO_PIN_PULLUP,NRF_GPIO_PIN_SENSE_LOW);NRF_POWER->SYSTEMOFF=1;while(true){};}while(digitalRead(POWER_KEY)==LOW)delay(5);}
 linkEvents=xQueueCreate(8,sizeof(LinkEvent));if(!linkEvents)while(true){delay(1000);}
 InternalFS.begin();loadSettings();Bluefruit.begin(1,0);Bluefruit.autoConnLed(false);Bluefruit.setName("RadioReminder");Bluefruit.setTxPower(0);Bluefruit.Periph.setConnectCallback(connected);Bluefruit.Periph.setDisconnectCallback(disconnected);Bluefruit.Periph.setConnInterval(80,160);
 Bluefruit.Security.setIOCaps(false,false,false);Bluefruit.Security.setMITM(false);
 service.begin();commandChar.setProperties(CHR_PROPS_WRITE);commandChar.setPermission(SECMODE_NO_ACCESS,SECMODE_ENC_NO_MITM);commandChar.setFixedLen(10);commandChar.setWriteCallback(commandWritten);commandChar.begin();
 statusChar.setProperties(CHR_PROPS_READ|CHR_PROPS_NOTIFY);statusChar.setPermission(SECMODE_ENC_NO_MITM,SECMODE_NO_ACCESS);statusChar.setFixedLen(10);statusChar.begin();notifyStatus();
 Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);Bluefruit.Advertising.addTxPower();Bluefruit.Advertising.addService(service);Bluefruit.ScanResponse.addName();Bluefruit.Advertising.restartOnDisconnect(true);Bluefruit.Advertising.setInterval(160,1600);Bluefruit.Advertising.setFastTimeout(30);Bluefruit.Advertising.start(0);bootAt=millis();
}
void loop(){
 uint32_t now=millis();linkTick();core.tick(now);buttonsTick();updateAudio();batteryTick();
 if(dirty&&now-saveAt>=5000)saveSettings();if(core.connected&&now-statusAt>=10000)notifyStatus();
 // Low-duty on-board LED: red=low battery; blue=disconnected or no valid time.
 bool blink=(lowBattery||!core.timeValid(now))&&now%5000<25;
 digitalWrite(LED_RED,blink&&lowBattery?LOW:HIGH);digitalWrite(LED_BLUE,blink&&!lowBattery?LOW:HIGH);
 delay(5);
}
