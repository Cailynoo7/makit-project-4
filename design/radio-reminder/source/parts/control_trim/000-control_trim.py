p=part('front_shell')
width=param('control_trim_width',13.4)
height=param('control_trim_height',21.5)
stroke=param('control_trim_stroke',0.65)
film=param('control_trim_film',0.12)
# Adhesive silver cut-film, connected U shape; installed on the outer surface.
trim=Pos(0,p.front_y-film/2,-1.0)*(Box(width,film,height)-Pos(0,0,stroke)*Box(width-2*stroke,film+0.1,height))
trim.color=srgb('#b7b9b8')
publish('control_trim',trim,'中部银色饰线',material='petg')