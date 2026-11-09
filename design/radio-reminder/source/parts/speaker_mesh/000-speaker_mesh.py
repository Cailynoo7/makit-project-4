p=part('front_shell')
diameter=param('acoustic_mesh_diameter',18.4)
thickness=param('acoustic_mesh_thickness',0.15)
# Real cut black acoustic fabric, bonded around its edge; not a speaker proxy.
cloth=Pos(-18,p.front_y+2.2,-5)*Rot(90,0,0)*Cylinder(diameter/2,thickness)
cloth.color=srgb('#101113')
publish('speaker_mesh',cloth,'黑色透声布',material='nylon')