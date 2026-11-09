import {board,keys,chargeUsb,amp,primarySide,serviceSide} from './layout';
import {boardPropsFromRules,groundPourPropsFromRules,keepoutPropsFromRules,netPropsFor,pcbRules} from './lib/makit-pcb';
const Rules=()=> <>
 {Object.keys(pcbRules.nets).map(n=><net key={n} name={n} {...netPropsFor(n)}/>)}
 {groundPourPropsFromRules().map(p=><copperpour key={p.name} {...p}/>)}
 {keepoutPropsFromRules().map(p=><keepout key={p.name} {...p}/>)}
</>;
const resistorData=[
 ['R_F1','10k','C25804',7,6,'PWM','LP1'],
 ['R_F2','10k','C25804',11,6,'LP1','LP2'],
 ['R_ATT','10k','C25804',9,1,'LP2','ATT'],
 ['R_DIV','1k','C21190',6,1,'ATT','GND'],
 ['R_SD','10k','C25804',18.5,-7,'AMP_EN','GND'],
 ['R_PWR','10k','C25804',5.8,-7,'V3V3','POWER'],
 ['R_CC1','5.1k','C23186',22,-3,'CC1','GND'],
 ['R_CC2','5.1k','C23186',22,-6,'CC2','GND']
] as const;
const capacitorData=[
 ['C_F1','10nF','C57112',7,9,'LP1','GND'],
 ['C_F2','10nF','C57112',10,9,'LP2','GND'],
 ['C_INP','100nF','C14663',23.8,1.8,'ATT','AINP'],
 ['C_INN','100nF','C14663',24.4,-3,'GND','AINN'],
 ['C_HF','1uF','C15849',13,-1,'VBAT','GND'],
 ['C_BULK1','10uF','C19702',18,6,'VBAT','GND'],
 ['C_BULK2','10uF','C19702',22,5,'VBAT','GND']
] as const;
export default()=> <board width={board.width} height={board.height} {...boardPropsFromRules()}>
 <Rules/>
 <schematicsection name="Interface" title="XIAO harness"/>
 <schematicsection name="Keys" title="Four keys"/>
 <schematicsection name="Audio" title="Filtered PWM audio"/>
 <connector name="USB_CHG" standard="usb_c" schSectionName="Interface" footprint="jlcpcb:C19188081" supplierPartNumbers={{jlcpcb:['C19188081']}}
  layer={serviceSide} pcbX={chargeUsb.x} pcbY={chargeUsb.y} pcbRotation={chargeUsb.rotation}
  connections={{GND1:'net.GND',GND2:'net.GND',GND3:'net.GND',GND4:'net.GND',EH:'net.GND',VBUS1:'net.USB5V',VBUS2:'net.USB5V',VBUS3:'net.USB5V',VBUS4:'net.USB5V',CC1:'net.CC1',CC2:'net.CC2'}}/>
 {(['sound','power'] as const).map((k,i)=><chip key={k} name={['SW_SOUND','SW_PWR'][i]} schSectionName="Keys"
  footprint="jlcpcb:C318884" supplierPartNumbers={{jlcpcb:['C318884']}} pcbX={keys[k].x} pcbY={keys[k].y} layer={k==='power'?serviceSide:primarySide}
  pinLabels={{pin1:'A1',pin2:'A2',pin3:'B1',pin4:'B2'}} connections={{A1:`net.${['SOUND','POWER'][i]}`,A2:`net.${['SOUND','POWER'][i]}`,B1:'net.GND',B2:'net.GND'}}/>) }
 {(['up','down'] as const).map((k,i)=><chip key={k} name={['SW_UP','SW_DOWN'][i]} schSectionName="Keys"
  footprint="jlcpcb:C393942" supplierPartNumbers={{jlcpcb:['C393942']}} pcbX={keys[k].x} pcbY={keys[k].y} pcbRotation={0} layer={primarySide}
  pinLabels={{pin1:'KEY',pin2:'GND',pin3:'SH1',pin4:'SH2'}} connections={{KEY:`net.${['VOL_UP','VOL_DOWN'][i]}`,GND:'net.GND',SH1:'net.GND',SH2:'net.GND'}}/>) }
 <chip name="U_AMP" schSectionName="Audio" footprint="jlcpcb:C113367" supplierPartNumbers={{jlcpcb:['C113367']}} pcbX={amp.x} pcbY={amp.y} pcbRotation={90}
  pinLabels={{pin1:'SD',pin2:'INN',pin3:'INP',pin4:'NC',pin5:'OUTP',pin6:'VDD',pin7:'GND',pin8:'OUTN'}}
  connections={{SD:'net.AMP_EN',INP:'net.AINP',INN:'net.AINN',OUTP:'net.SPKP',VDD:'net.VBAT',GND:'net.GND',OUTN:'net.SPKN'}}/>
 {resistorData.map(([name,value,code,x,y,a,b])=><resistor key={name} name={name} resistance={value} schSectionName="Audio" footprint={`jlcpcb:${code}`} supplierPartNumbers={{jlcpcb:[code]}} pcbX={x} pcbY={y} connections={{pin1:`net.${a}`,pin2:`net.${b}`}}/>)}
 {capacitorData.map(([name,value,code,x,y,a,b])=><capacitor key={name} name={name} capacitance={value} schSectionName="Audio" footprint={`jlcpcb:${code}`} supplierPartNumbers={{jlcpcb:[code]}} pcbX={x} pcbY={y} pcbRotation={90} connections={{pin1:`net.${a}`,pin2:`net.${b}`}}/>)}
 {([['TP1',-22,-1,'VBAT'],['TP2',-18,-1,'GND'],['TP3',7,-10,'SPKP'],['TP4',20,-10,'SPKN'],
 ['TP5',3,2,'PWM'],['TP6',-6,-10,'AMP_EN'],['TP7',-9,-7,'SOUND'],
 ['TP9',-9,-4,'VOL_UP'],['TP10',-6,-4,'VOL_DOWN'],['TP11',-9,-1,'POWER'],['TP12',-6,-1,'V3V3'],['TP13',-6,2,'USB5V']] as const).map(([name,x,y,n])=><testpoint key={name} name={name} doNotPlace schSectionName="Interface" footprintVariant="through_hole" padDiameter="2mm" holeDiameter="1mm" pcbX={x} pcbY={y} connections={{pin1:`net.${n}`}}/>)}

</board>;
