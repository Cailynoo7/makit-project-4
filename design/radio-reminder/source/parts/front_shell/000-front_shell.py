from screwjoint import make_screw_joint_v1
from splitjoint import attach_panel_v1, SplitJointNotApplicableV1
case_width=param('case_width',60.0)
case_depth=param('case_depth',17.5)
case_height=param('case_height',34.0)
edge_radius=param('edge_radius',1.6)
front_edge_radius=param('front_edge_radius',1.2)
rear_edge_radius=param('rear_edge_radius',0.6)
reinforcement_outer_inset=param('reinforcement_outer_inset',0.35)
wall=param('wall',2.4)
handle_width=param('handle_width',58.0)
handle_height=param('handle_height',10.0)
handle_bar=param('handle_bar',3.4)
handle_depth=param('handle_depth',3.6)
handle_outer_radius=param('handle_outer_radius',2.0)
handle_inner_radius=param('handle_inner_radius',1.4)
handle_edge_radius=param('handle_edge_radius',0.45)
handle_root_embed=param('handle_root_embed',0.6)
lanyard_outer_radius=param('lanyard_outer_radius',3.8)
lanyard_hole_radius=param('lanyard_hole_radius',2.0)
lanyard_flat_offset=param('lanyard_flat_offset',0.2)
lanyard_root_radius=param('lanyard_root_radius',0.6)
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
front=fillet(front.edges().group_by(Axis.Y)[0],front_edge_radius)
front=fillet(front.edges().group_by(Axis.Y)[-1],0.1)
front-=Pos(0,2,0)*Box(case_width-2*wall,case_depth,case_height-2*wall)
# Rounded U-frame with consistent section and an integrated closed half-round lanyard eye.
handle_top=case_height/2+handle_height
handle_bottom=case_height/2-handle_root_embed
handle=Pos(0,0,(handle_top+handle_bottom)/2)*Box(handle_width,handle_depth,handle_top-handle_bottom)
handle=fillet([e for e in handle.edges().filter_by(Axis.Y) if e.center().Z>handle_top-0.01],handle_outer_radius)
inner_top=handle_top-handle_bar
inner_bottom=handle_bottom-1.0
handle_opening=Pos(0,0,(inner_top+inner_bottom)/2)*Box(handle_width-2*handle_bar,handle_depth+2,inner_top-inner_bottom)
handle_opening=fillet([e for e in handle_opening.edges().filter_by(Axis.Y) if e.center().Z>inner_top-0.01],handle_inner_radius)
handle-=handle_opening
lug_bottom=handle_top-handle_root_embed
lug_top=handle_top+lanyard_outer_radius
lug=Pos(0,0,handle_top)*Rot(90,0,0)*Cylinder(lanyard_outer_radius,handle_depth)
lug=lug & (Pos(0,0,(lug_bottom+lug_top)/2)*Box(2*lanyard_outer_radius+2,handle_depth+2,lug_top-lug_bottom))
handle+=lug
root_edges=[e for e in handle.edges().filter_by(Axis.Y) if abs(e.center().Z-handle_top)<0.01 and abs(e.center().X)<lanyard_outer_radius+1]
assert len(root_edges)==2
handle=fillet(root_edges,lanyard_root_radius)
hole_flat=handle_top+lanyard_flat_offset
hole=Pos(0,0,hole_flat)*Rot(90,0,0)*Cylinder(lanyard_hole_radius,handle_depth+2)
hole=hole & (Pos(0,0,hole_flat+lanyard_hole_radius/2)*Box(2*lanyard_hole_radius+2,handle_depth+4,lanyard_hole_radius))
handle-=hole
rim_edges=[e for e in handle.edges() if abs(abs(e.center().Y)-handle_depth/2)<0.00001]
handle=fillet(rim_edges,handle_edge_radius)
assert len(handle.solids())==1
print('Handle with lanyard eye',handle.bounding_box())
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
# Recessed reinforcement follows the shared rounded outline without refilling the exterior roll.
strip_inner=case_width/2-3.7
strip_outer=case_width/2-reinforcement_outer_inset
strip_width=strip_outer-strip_inner
strip_center=(strip_outer+strip_inner)/2
profile_keep=fillet(Box(case_width,case_depth+6,case_height).edges().filter_by(Axis.Y),edge_radius)
for x in (-1,1):
    reinforcement=Pos(x*strip_center,seam_y-2.825,closure_z)*Box(strip_width,5.95,5.0)
    front+=reinforcement & profile_keep
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
lid=fillet(lid.edges().group_by(Axis.Y)[-1],rear_edge_radius)
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
usb_x=param('usb_opening_center_x',17.4)
usb_z=param('usb_opening_center_z',9.0)
usb_w=param('usb_opening_width',11.8)
usb_h=param('usb_opening_height',5.4)
# True capsule opening: semicircular ends, sufficient clearance around the real socket.
lid-=Pos(usb_x,case_depth/2-0.5,usb_z)*Rot(90,0,0)*extrude(SlotOverall(usb_w,usb_h),amount=2.5,both=True)
usb_visible_w=param('usb_visible_opening_width',9.54)
usb_visible_h=param('usb_visible_opening_height',3.76)
usb_visible_r=param('usb_visible_corner_radius',1.6)
usb_lip_inner_y=param('usb_lip_inner_y',8.45)
usb_lip_outer_y=param('usb_lip_outer_y',9.15)
# Outer close-fitting mouth; the deeper capsule clears the socket body.
usb_lip_mid=(usb_lip_inner_y+usb_lip_outer_y)/2
usb_lip_t=usb_lip_outer_y-usb_lip_inner_y
usb_rim=Pos(usb_x,usb_lip_mid,usb_z)*Rot(90,0,0)*extrude(SlotOverall(usb_w+0.4,usb_h+0.4),amount=usb_lip_t/2,both=True)
usb_throat=Pos(usb_x,usb_lip_mid,usb_z)*Rot(90,0,0)*extrude(RectangleRounded(usb_visible_w,usb_visible_h,usb_visible_r),amount=usb_lip_t/2+0.1,both=True)
lid+=usb_rim-usb_throat
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
