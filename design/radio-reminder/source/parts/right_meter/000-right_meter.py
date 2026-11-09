p=part('front_shell')
w=param('right_meter_width',5.8)
h=param('right_meter_height',2.4)
plate=Pos(-14,p.front_y-0.48,p.tuner_z)*Box(w,0.08,h)
# Radial engraved marks suggest the two VU meters in the reference.
for a in (-60,-30,0,30,60):
    mark=Pos(-14,p.front_y-0.53,p.tuner_z-0.8)*Rot(0,a,0)*Pos(0,0,1.1)*Box(0.23,0.06,0.7)
    plate-=mark
plate.color=srgb('#d7d1b1')
publish('right_meter',plate,'电平表装饰',material='petg')