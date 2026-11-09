from screwjoint import make_screw_joint_v1
from splitjoint import attach_panel_v1, SplitJointNotApplicableV1
case_width=param('case_width',60.0)
case_depth=param('case_depth',17.5)
case_height=param('case_height',34.0)
edge_radius=param('edge_radius',0.8)
wall=param('wall',2.4)
handle_width=param('handle_width',58.0)
handle_height=param('handle_height',10.0)
handle_bar=param('handle_bar',2.5)
grille_radius=param('grille_radius',8.8)
grille_hole=param('grille_hole',1.6)
button_gap=param('button_gap',0.25)
front_key_z=param('front_function_key_z',-4.0)
front_key_size=param('front_function_key_size',6.2)
carrier_y=param('carrier_plane_y',-0.3)
top_key_y=carrier_y-1.65
power_x=param('power_key_x',7.0)
power_z=param('power_key_z',9.0)
closure_x=param('symmetric_closure_x',28.0)
closure_z=param('symmetric_closure_z',13.8)
hook_x=param('lower_hook_x',18.0)
hook_width=param('lower_hook_width',5.0)
front_y=-case_depth/2
seam_gap=param('rear_seam_gap',0.15)
lap_length=param('rear_lap_length',0.85)
lap_clearance=param('rear_lap_clearance',0.2)
seam_y=case_depth/2-2.0-seam_gap
front=Pos(0,(front_y+seam_y)/2,0)*Box(case_width,seam_y-front_y,case_height)
front=fillet(front.edges().filter_by(Axis.Y),edge_radius)
front=fillet(front.edges().group_by(Axis.Y)[0],edge_radius)
front=fillet(front.edges().group_by(Axis.Y)[-1],0.1)
front-=Pos(0,2,0)*Box(case_width-2*wall,case_depth,case_height-2*wall)
handle=Pos(0,0,case_height/2+handle_height/2-0.3)*Box(handle_width,handle_bar,handle_height+0.6)
handle-=Pos(0,0,case_height/2+handle_height/2-0.8)*Box(handle_width-2*handle_bar,handle_bar+2,handle_height-handle_bar+1)
handle=fillet(handle.edges().filter_by(Axis.Y),0.35)
front+=handle
speaker_x=(-18.0,18.0)
speaker_z=-5.0
for sx in speaker_x:
    front-=Pos(sx,front_y+0.2,speaker_z)*Rot(90,0,0)*Cylinder(10.0,0.8)
    for row in range(-3,4):
        for col in range(-3,4):
            xx=col*2.5+(1.25 if row%2 else 0)
            zz=row*2.3
            if xx*xx+zz*zz < 7.9*7.9:
                depth=wall+1 if sx<0 else wall-0.5
                front-=Pos(sx+xx,front_y+depth/2-0.1,speaker_z+zz)*Rot(90,0,0)*Cylinder(grille_hole/2,depth+0.2)
front-=Pos(0,front_y+wall/2,front_key_z)*Box(front_key_size+2*button_gap,wall+2,front_key_size+2*button_gap)
for x in (-18.0,-7.0):
    front-=Pos(x,top_key_y,case_height/2-0.2)*Box(5.4,4.0,6.0)
sleeve=Pos(-18,front_y+wall+1.7,-5)*Rot(90,0,0)*(Cylinder(10.0,4.2)-Cylinder(9.35,5))
front+=sleeve
# Rear-open locating rails: PCB enters from the rear, then the lid retains it.
for x in (-1,1):
    front+=Pos(x*27.2,carrier_y+0.6,0)*Box(1.8,4.5,26.5)
    front+=Pos(x*26.6,carrier_y-1.6,8 if x<0 else 0)*Box(2.4,1.0,10.0 if x<0 else 26.5)
# Clear purchased speaker maximum envelope at the left guide, and XIAO USB on right.
front-=Pos(-18,front_y+4.75,-5)*Rot(90,0,0)*Cylinder(9.3,5.5)
front-=Pos(26.8,2.3,-3.5)*Box(2.4,3.0,10.0)
# Symmetric reinforced wall strips stay outside protected battery / XIAO envelopes.
for x in (-1,1):
    front+=Pos(x*28.15,seam_y-2.825,closure_z)*Box(3.7,5.95,5.0)
# Hidden lower hooks: pocket floor remains 1 mm thick, lip locks the hook foot.
for x in (-hook_x,hook_x):
    front-=Pos(x,5.6,-15.3)*Box(hook_width+0.5,2.4,1.4)
    front+=Pos(x,6.1,-15.5)*Box(hook_width+0.5,1.0,0.6)
# Normalize boolean list output for the connection kits.
if isinstance(front, ShapeList):
    front=Compound(children=list(front))
print('Front solids before attachments',[(s.volume,str(s.bounding_box())) for s in front.solids()])
# Tuner panel: a real locating/gluing connection, sized from the reference.
tuner_w=52.0
tuner_h=5.2
tuner_z=13.0
panel=Pos(0,front_y-0.3,tuner_z)*Box(tuner_w,0.6,tuner_h)
attachment_mode='splitjoint_glue_pad'
try:
    attachment=attach_panel_v1(panel=panel,carrier=front,method='glue_pad',material='PETG',label='tuner_panel')
except SplitJointNotApplicableV1 as exc:
    attachment_mode='free_glue_recess'
    print('Tuner attachment kit rejected:',str(exc))
    front-=Pos(0,front_y+0.15,tuner_z)*Box(tuner_w+0.3,0.5,tuner_h+0.3)
    panel=Pos(0,0.3,0)*panel
else:
    front=attachment.carrier
    panel=attachment.panel
tuner_panel_shape=panel
# Lid and shallow locating lip, notched around the battery pack and closure bosses.
lid_t=2.0
# Flush outside faces, crisp mating edges. Outer-wall lap does not consume the hardware cavity.
lid=Pos(0,case_depth/2-lid_t/2,0)*Box(case_width,lid_t,case_height)
lid=fillet(lid.edges().filter_by(Axis.Y),edge_radius)
lid=fillet(lid.edges().group_by(Axis.Y)[-1],0.35)
lid=fillet(lid.edges().group_by(Axis.Y)[0],0.1)
lap_outer_w=case_width-1.6
lap_outer_h=case_height-1.6
lap_inner_w=case_width-2*wall
lap_inner_h=case_height-2*wall
lap_start=seam_y-0.05
lap_end=seam_y+lap_length
lap=Pos(0,(lap_start+lap_end)/2,0)*(Box(lap_outer_w,lap_end-lap_start,lap_outer_h)-Box(lap_inner_w,lap_end-lap_start+0.2,lap_inner_h))
channel_front=case_depth/2-lid_t-0.05
channel_back=lap_end+lap_clearance
channel=Pos(0,(channel_front+channel_back)/2,0)*(Box(lap_outer_w+2*lap_clearance,channel_back-channel_front,lap_outer_h+2*lap_clearance)-Box(lap_inner_w-2*lap_clearance,channel_back-channel_front+0.2,lap_inner_h-2*lap_clearance))
# Keep the complete countersink land and the kit's closure bosses.
for x in (-closure_x,closure_x):
    lap-=Pos(x,case_depth/2-2.0,closure_z)*Box(8.0,4,8.0)
    channel-=Pos(x,case_depth/2-2.0,closure_z)*Box(8.0,4,7.4)
# Bottom hooks replace the lap below z=-10, giving clearance for the opening tilt.
lap-=Pos(0,case_depth/2-2.0,-14)*Box(case_width+2,4,8)
channel-=Pos(0,case_depth/2-2.0,-14.3)*Box(case_width+2,4,8)
front+=lap
lid-=channel
lid-=Pos(power_x,case_depth/2-0.4,power_z)*Rot(90,0,0)*Cylinder(3.0,5)
lid-=Pos(17.4,case_depth/2-0.5,9.0)*Box(11.0,5,5.0)
post_front=carrier_y+0.3
post_back=case_depth/2-1.9
lid+=Pos(10,(post_front+post_back)/2,-13.55)*Box(3.0,post_back-post_front,1.8)
# Hook tongues and feet are integral with the lid, concealed behind the bottom seam.
for x in (-hook_x,hook_x):
    lid+=Pos(x,5.9,-14.6)*Box(hook_width,1.9,0.8)
    lid+=Pos(x,5.0,-15.15)*Box(hook_width,0.8,1.3)
joint_records=[]
for i,x in enumerate((-closure_x,closure_x)):
    joint=make_screw_joint_v1(size='M2',at=Location((x,case_depth/2,closure_z),(-90,0,0)),through=[(lid,lid_t)],seat='countersink',head_recess=0.0,engage_depth=4.0,into=front,strategy='self_forming',boss='none',material='PETG',finish='black',label='rear_closure_'+str(i))
    front=front+joint.bosses-joint.engage_cuts
    lid=lid-joint.through_cuts[0]
    joint_records.append(joint.bom)
    print('Flush countersink',i,'opening_diameter',joint.seat_opening_diameter_mm,'head_diameter',joint.head_diameter_mm,'head_above_surface',joint.head_above_surface_mm,'length',joint.screw_length_mm)
    joint.hardware.color=srgb('#303133')
    publish('rear_screw_'+str(i),joint.hardware,'后盖螺钉',role='fastener',material='steel')
rear_lid_shape=lid
print('Closure BOM',joint_records)
print('Tuner connection',attachment_mode)
front.color=srgb('#151618')
publish('front_shell',front,'收音机前壳',material='petg')
