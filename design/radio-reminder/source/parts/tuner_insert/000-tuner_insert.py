p=part('front_shell')
w=param('tuner_insert_width',50.0)
h=param('tuner_insert_height',3.3)
film=param('tuner_insert_thickness',0.18)
plate=Pos(0,p.front_y-0.35,p.tuner_z)*Box(w,film,h)
# These shallow marks are moulded into the dial substrate.
for x in range(-8,24,2):
    plate-=Pos(x,p.front_y-0.39,p.tuner_z+0.65)*Box(0.35,0.1,0.9 if x%4==0 else 0.5)
plate.color=srgb('#202124')
publish('tuner_insert',plate,'调谐刻度底片',material='petg')