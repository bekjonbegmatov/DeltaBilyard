import bpy, math, random, os
from mathutils import Vector
ROOT='/Users/behruz/Documents/ChatGPT/DeltaBilyard'
random.seed(8)
scene=bpy.data.scenes.new('DeltaBilyard Atelier')
bpy.context.window.scene=scene

def enum(cls,prop,value):
    return next(i.identifier for i in cls.bl_rna.properties[prop].enum_items if i.identifier==value)

def material(name,color,rough=.4,metal=0):
    m=bpy.data.materials.new(name);m.use_nodes=True
    p=next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
    p.inputs['Base Color'].default_value=(*color,1);p.inputs['Roughness'].default_value=rough;p.inputs['Metallic'].default_value=metal
    m.diffuse_color=(*color,1)
    return m
wood=material('American walnut',(.13,.058,.026),.3)
brass=material('Brushed antique brass',(.48,.32,.12),.29,.8)
felt=material('Cloth',(.017,.19,.12),.92)
rubber=material('Cushion',(.012,.105,.063),.72)
leather=material('Pocket leather',(.018,.012,.009),.8)
black=material('Pocket darkness',(.003,.004,.003),1)
ivory=material('Ivory sights',(.82,.76,.57),.3)
wall=material('Deep green wall',(.025,.049,.036),.95)
trim=material('Wall walnut',(.075,.033,.016),.5)
cloth=material('Lounge upholstery',(.13,.025,.018),.9)
# Embedded wood texture, independent of external files.
import numpy as np
size=256
xx,yy=np.meshgrid(np.arange(size),np.arange(size))
grains=(np.sin(xx*.24+np.sin(yy*.035)*1.5)+np.sin(xx*.71+np.sin(yy*.019)*3))*.018
pixels=np.zeros((size,size,4),dtype=np.float32)
for k,base in enumerate([.19,.092,.041]):pixels[:,:,k]=np.clip(base+grains*(1 if k==0 else .65),0,1)
pixels[:,:,3]=1
image=bpy.data.images.new('Walnut grain',width=size,height=size);image.pixels.foreach_set(pixels.ravel());image.pack()
node=wood.node_tree.nodes.new('ShaderNodeTexImage');node.image=image
wood.node_tree.links.new(node.outputs['Color'],next(n for n in wood.node_tree.nodes if n.type=='BSDF_PRINCIPLED').inputs['Base Color'])
table=[];room=[]
def cube(name,loc,scale,mat,bevel=.0,group=table):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name;o.dimensions=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    o.data.materials.append(mat)
    if bevel:
        mod=o.modifiers.new('Soft manufactured edges',enum(bpy.types.Modifier,'type','BEVEL'));mod.width=bevel;mod.segments=3
        bpy.ops.object.modifier_apply(modifier=mod.name)
    group.append(o);return o

def cylinder(name,loc,radius,depth,mat,group=table,vertices=48):
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=radius,depth=depth,location=loc);o=bpy.context.object;o.name=name;o.data.materials.append(mat);group.append(o)
    for p in o.data.polygons:p.use_smooth=True
    return o

def cut(target,tool):
    bpy.context.view_layer.objects.active=target
    m=target.modifiers.new('Precision pocket cut',enum(bpy.types.Modifier,'type','BOOLEAN'));m.operation=enum(bpy.types.BooleanModifier,'operation','DIFFERENCE');m.object=tool
    bpy.ops.object.modifier_apply(modifier=m.name)

def ring(name,x,y,z,r,minor,mat,group=table):
    bpy.ops.mesh.primitive_torus_add(major_segments=48,minor_segments=10,location=(x,y,z),major_radius=r,minor_radius=minor)
    o=bpy.context.object;o.name=name;o.data.materials.append(mat);group.append(o)
    for p in o.data.polygons:p.use_smooth=True
    return o

body=cube('Solid walnut cabinet',(0,0,-.17),(2.90,1.63,.25),wood,.035)
rail=cube('Walnut rail frame',(0,0,.025),(2.90,1.63,.085),wood,.018)
opening=cube('temporary bed opening',(0,0,.10),(2.66,1.39,.30),black)
cut(rail,opening);table.remove(opening);bpy.data.objects.remove(opening,do_unlink=True)
bed=cube('Tournament cloth',(0,0,-.007),(2.54,1.27,.026),felt)
# Pocket throats sit BEHIND the nose lines, with open tapered mouths.
# Playable surface is 2.54 x 1.27; balls are 57.15 mm in diameter.
pockets=[(-1.310,-.675),(0,-.680),(1.310,-.675),(-1.310,.675),(0,.680),(1.310,.675)]
def prism(name,points,z0,z1,mat):
    n=len(points);verts=[(x,y,z) for z in (z0,z1) for x,y in points]
    faces=[tuple(reversed(range(n))),tuple(range(n,2*n))]+[(j,(j+1)%n,(j+1)%n+n,j+n) for j in range(n)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update()
    o=bpy.data.objects.new(name,mesh);scene.collection.objects.link(o);o.data.materials.append(mat);table.append(o);return o
for i,(x,y) in enumerate(pockets):
    sx=1 if x>0 else -1;sy=1 if y>0 else -1
    radius=.061 if x else .064
    cutter=cylinder('temporary pocket cutter',(x,y,-.08),radius,.70,black)
    for o in [rail,bed,body]:cut(o,cutter)
    table.remove(cutter);bpy.data.objects.remove(cutter,do_unlink=True)
    if x:
        pts=[(sx*1.192,sy*.634),(sx*1.269,sy*.557),(sx*1.364,sy*.652),(sx*1.287,sy*.729)]
    else:
        pts=[(-.068,sy*.634),(.068,sy*.634),(.053,sy*.735),(-.053,sy*.735)]
    # Sort around centroid to maintain a convex, correctly wound cutter.
    cx=sum(p[0] for p in pts)/4;cy=sum(p[1] for p in pts)/4
    pts.sort(key=lambda p:math.atan2(p[1]-cy,p[0]-cx))
    cutter=prism('temporary pocket mouth',pts,-.40,.30,black)
    for o in [rail,bed,body]:cut(o,cutter)
    table.remove(cutter);bpy.data.objects.remove(cutter,do_unlink=True)
    # Dark shelf follows the funnel, below the cloth. No cylinders above the bed.
    prism('Pocket %d mouth shadow'%i,pts,-.035,-.030,black)
    cylinder('Pocket %d bottom'%i,(x,y,-.10),radius,.008,black)
    # Inner leather bore stops at felt height, never floats above playing surface.
    verts=[];faces=[];n=48
    for z in [.005,-.10]:
        for j in range(n):
            a=j*math.tau/n;verts.append((x+radius*math.cos(a),y+radius*math.sin(a),z))
    for j in range(n):faces.append((j,(j+1)%n,(j+1)%n+n,j+n))
    mesh=bpy.data.meshes.new('Pocket lining');mesh.from_pydata(verts,[],faces);mesh.update()
    o=bpy.data.objects.new('Pocket %d recessed throat'%i,mesh);scene.collection.objects.link(o);o.data.materials.append(black);table.append(o)
# Six actual cushions, with tapered jaws toward each open pocket.
def cushion(name,x0,x1,y,sign):
    # Nose at the physical collision line, exposed over the wood opening.
    pts=[(x0,y,.009),(x1,y,.009),(x1-.015,y+sign*.06,.009),(x0+.015,y+sign*.06,.009)]
    verts=pts+[(x0,y,.035),(x1,y,.035),(x1-.015,y+sign*.06,.055),(x0+.015,y+sign*.06,.055)]
    faces=[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update();o=bpy.data.objects.new(name,mesh);scene.collection.objects.link(o);o.data.materials.append(rubber);table.append(o)
    bpy.context.view_layer.objects.active=o;o.select_set(True);mod=o.modifiers.new('Rounded cushion edge',enum(bpy.types.Modifier,'type','BEVEL'));mod.width=.005;mod.segments=3;bpy.ops.object.modifier_apply(modifier=mod.name);o.select_set(False)
for sign in [-1,1]:
    cushion('Long cushion left',-1.192,-.068,sign*.635,sign)
    cushion('Long cushion right',.068,1.192,sign*.635,sign)
    cushion('End cushion',-.557,.557,sign*1.27,sign)
    short=table[-1];short.rotation_euler[2]=-math.pi/2
for y in [-.755,.755]:
    for x in [-.9525,-.635,-.3175,.3175,.635,.9525]:
        o=cube('Mother of pearl diamond',(x,y,.069),(.013,.022,.002),ivory,.001);o.rotation_euler[2]=math.pi/4
for x in [-1.39,1.39]:
    for y in [-.3175,0,.3175]:cube('End rail diamond',(x,y,.069),(.018,.013,.002),ivory,.001)
for x in [-1.07,1.07]:
    for y in [-.50,.50]:
        cube('Tapered walnut leg',(x,y,-.58),(.16,.16,.64),wood,.02)
        cube('Brass levelling foot',(x,y,-.91),(.18,.18,.045),brass,.012)
cube('Cabinet brass accent',(0,-.818,-.10),(2.78,.008,.008),brass,.002)
cube('Cabinet brass accent',(0,.818,-.10),(2.78,.008,.008),brass,.002)
# Club interior with parquet, panelling, lounge seats and cue rack.
floors=[material('Parquet '+str(i),(.025+i*.0015,.022+i*.001,.018+i*.0008),.78) for i in range(8)]
for row in range(38):
    for col in range(12):
        x=-4+col*.72+(row%2)*.36
        cube('Oak parquet',(x,-3+row*.17,-.96),(.718,.168,.045),random.choice(floors),.003,room)
# Reuse the eight parquet meshes; GLB stores each plank geometry only once.
floor_meshes={}
for o in room:
    if o.name.startswith('Oak parquet'):
        key=o.data.materials[0].name
        if key in floor_meshes:o.data=floor_meshes[key]
        else:floor_meshes[key]=o.data
for x in [-4.0,4.0]:cube('Side wall',(x,0,.75),(.1,6.8,3.5),wall,.0,room)
for y in [-3.3,3.3]:cube('End wall',(0,y,.75),(8.1,.1,3.5),wall,.0,room)
for y in [-3.23,3.23]:
    cube('Walnut wainscot',(0,y,-.40),(8,.07,1.1),trim,.01,room)
    cube('Brass wall line',(0,y,.17),(8,.018,.018),brass,.003,room)
    for x in [-3.6,-2.4,-1.2,0,1.2,2.4,3.6]:
        cube('Wall pilaster',(x,y,.65),(.075,.11,3.1),wood,.009,room)
        # Framed dark green inset panels, no external textures.
        cube('Gallery frame',(x+.50,y- .06 if y>0 else y+.06,1.1),(.7,.035,1.0),brass,.01,room)
        cube('Gallery inset',(x+.50,y- .085 if y>0 else y+.085,1.1),(.64,.02,.94),wall,.005,room)
for x in [-2.6,2.6]:
    cube('Leather club sofa seat',(x,2.4,-.52),(1.7,.70,.25),cloth,.08,room)
    cube('Leather club sofa back',(x,2.72,-.12),(1.7,.18,.72),cloth,.06,room)
    for dx in [-.78,.78]:cube('Sofa arm',(x+dx,2.4,-.24),(.18,.75,.48),cloth,.06,room)
for i in range(5):
    o=cylinder('Wall cue '+str(i),(-2.9+i*.10,-3.10,.05),.009,1.5,wood,room,12);o.rotation_euler[1]=.08
cube('Cue rack lower',(-2.7,-3.08,-.65),(.65,.15,.07),wood,.015,room)
cube('Cue rack upper',(-2.7,-3.08,.65),(.65,.10,.05),wood,.01,room)
for x in [-.7,0,.7]:
    bpy.ops.mesh.primitive_cone_add(vertices=48,radius1=.22,radius2=.065,depth=.16,location=(x,0,1.65));o=bpy.context.object;o.name='Pendant shade';o.data.materials.append(brass);room.append(o)
    cylinder('Pendant suspension',(x,0,2.12),.007,.80,black,room,12)
# Camera and studio lighting for inspection. Not included in GLB export.
def aim(o,at):o.rotation_euler=(Vector(at)-o.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add(location=(2.7,-2.6,2.15));scene.camera=bpy.context.object;scene.camera.data.lens=43;aim(scene.camera,(0,0,-.13))
for loc,energy,size in [((0,0,3),400,3),((-2,-1,2),150,2),((2,2,2),200,2)]:
    bpy.ops.object.light_add(type=enum(bpy.types.Light,'type','AREA'),location=loc);light=bpy.context.object;light.data.energy=energy;light.data.shape=enum(bpy.types.AreaLight,'shape','DISK');light.data.size=size;aim(light,(0,0,0))
scene.world=bpy.data.worlds.new('Club ambient');scene.world.use_nodes=True
bg=next(n for n in scene.world.node_tree.nodes if n.type=='BACKGROUND');bg.inputs[0].default_value=(.08,.10,.08,1);bg.inputs[1].default_value=.35
scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100
scene.render.image_settings.file_format=enum(bpy.types.ImageFormatSettings,'file_format','PNG')
# Export only the intended assets, preserving the user's original scene.
def export(objects,name):
    for o in scene.objects:o.select_set(False)
    for o in objects:o.select_set(True)
    bpy.context.view_layer.objects.active=objects[0]
    bpy.ops.export_scene.gltf(filepath=ROOT+'/frontend/public/models/'+name,export_format='GLB',use_selection=True,use_active_scene=True,export_apply=True)
export(table,'club-table.glb');export(room,'club-room.glb')
for o in scene.objects:o.select_set(False)
bpy.context.view_layer.objects.active=body;body.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=ROOT+'/assets/blender/delta-club.blend')
print('Exported club-table.glb and club-room.glb; table meshes:',len(table),'room meshes:',len(room))
