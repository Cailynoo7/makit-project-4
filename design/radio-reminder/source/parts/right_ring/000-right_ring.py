p=part('front_shell')
ring_outer=param('right_ring_outer_diameter',19.8)
ring_inner=param('right_ring_inner_diameter',18.1)
ring_thickness=param('right_ring_thickness',0.7)
ring=Rot(90,0,0)*(Cylinder(ring_outer/2,ring_thickness)-Cylinder(ring_inner/2,ring_thickness+1))
ring=Pos(18,p.front_y+0.05,-5)*ring
ring.color=srgb('#b7b9b8')
publish('right_ring',ring,'右网罩银圈',material='petg')