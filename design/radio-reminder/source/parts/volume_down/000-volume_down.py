p=part('front_shell')
width=param('volume_down_width',5.0)
depth=param('volume_down_depth',3.5)
tip_gap=param('volume_down_tip_gap',0.2)
head=Pos(-7,p.top_key_y,15.85)*Box(width,depth,3.7)
head=fillet(head.edges(),0.25)
head+=Pos(-7,p.top_key_y,14.2)*Box(width+2,depth+2,0.6)
z0=12.474+tip_gap
head+=Pos(-7,p.top_key_y,(z0+14.2)/2)*Box(1.8,1.0,14.2-z0+0.1)
head-=Pos(-7,p.top_key_y,17.65)*Box(2.6,0.55,0.35)

head.color=srgb('#202124')
publish('volume_down',head,'音量减键',material='petg')