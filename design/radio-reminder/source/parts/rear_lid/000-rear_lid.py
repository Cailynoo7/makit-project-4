p=part('front_shell')
notch_depth=param('service_grip_depth',0.35)
guide_d=param('power_guide_outer_diameter',8.4)
board_w=param('xiao_board_width',17.78)
board_l=param('xiao_board_length',20.955)
lid=p.rear_lid_shape
lid-=Pos(0,p.case_depth/2-notch_depth/2,-13.0)*Box(22.0,notch_depth+0.02,4.0)
lid-=Pos(p.power_x,p.case_depth/2-1.45,p.power_z)*Rot(90,0,0)*Cylinder(3.8,1.0)
lid+=Pos(p.power_x,p.case_depth/2-2.1,p.power_z)*Rot(90,0,0)*(Cylinder(3.6,0.3)-Cylinder(1.5,0.5))
for x in (10.0,22.0):
    lid+=Pos(x,p.case_depth/2-2.05,-3.5)*Box(3.0,0.3,10.0)
lid+=Pos(15.4,p.case_depth/2-2.65,-13.1)*Box(18,1.5,0.8)
pieces=lid.solids()
lid=pieces[0].fuse(*pieces[1:]) if len(pieces)>1 else pieces[0]
lid.color=srgb('#151618')
publish('rear_lid',lid,'薄型后盖与固定面',material='petg')