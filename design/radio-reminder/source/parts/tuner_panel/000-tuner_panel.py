p=part('front_shell')
inset=param('tuner_border',0.85)
depth=param('tuner_recess_depth',0.25)
panel=p.tuner_panel_shape
panel-=Pos(0,p.front_y-0.55+depth/2,p.tuner_z)*Box(p.tuner_w-2*inset,depth+0.1,p.tuner_h-2*inset)
panel.color=srgb('#b7b9b8')
publish('tuner_panel',panel,'调谐窗饰板',material='petg')