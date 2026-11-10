p=part('front_shell')
key_size=p.front_key_size
key_protrusion=param('sound_key_protrusion',0.7)
tip_gap=param('sound_tip_gap',0.2)
head=Pos(0,p.front_y+1.0,p.front_key_z)*Box(key_size,3.4,key_size)
head=fillet(head.edges(),0.3)
head+=Pos(0,p.front_y+p.wall+0.4,p.front_key_z)*Box(key_size+2,0.6,key_size+2)
stem_start=p.front_y+p.wall+0.7
stem_end=p.carrier_y-2.545-tip_gap
head+=Pos(0,(stem_start+stem_end)/2,p.front_key_z)*Rot(90,0,0)*Cylinder(1.2,stem_end-stem_start+0.1)
triangle_radius=param('sound_triangle_radius',1.7)
triangle_height=param('sound_triangle_height',0.25)
red_coat=param('sound_triangle_red_coat',0.05)
assert 0 < red_coat < triangle_height
triangle_start=p.front_y-0.6
tri=Pos(0,triangle_start,p.front_key_z)*Rot(90,0,0)*extrude(RegularPolygon(triangle_radius,3,rotation=0),triangle_height-red_coat)
head+=tri
# Red acrylic finish on the raised triangle, not a separate printed or assembled button.
triangle=Pos(0,triangle_start-(triangle_height-red_coat),p.front_key_z)*Rot(90,0,0)*extrude(RegularPolygon(triangle_radius,3,rotation=0),red_coat)
triangle.color=srgb('#c22b43')
publish('sound_triangle',triangle,'三角标识红色涂层',material='acrylic')
head.color=srgb('#202124')
publish('sound_key',head,'换音多功能键帽',material='petg')
