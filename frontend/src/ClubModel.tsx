import {useMemo,useEffect} from 'react';
import {surfaceTextures} from './surfaceTextures';
import {useGLTF} from '@react-three/drei';
import * as THREE from 'three';
import {mergeGeometries} from 'three/examples/jsm/utils/BufferGeometryUtils.js';
function optimized(source:THREE.Object3D,cloth:string,venue:string,room:boolean,textures:ReturnType<typeof surfaceTextures>){
 const groups=new Map<string,{geometries:THREE.BufferGeometry[];material:THREE.Material;name:string}>();source.updateMatrixWorld(true);
 source.traverse(node=>{if(!(node instanceof THREE.Mesh))return;if(node.name.startsWith('Pendant'))return;
  const materials=Array.isArray(node.material)?node.material:[node.material];
  // Split multi-material booleans by draw group before static batching.
  for(let i=0;i<materials.length;i++){const material=materials[i] as THREE.MeshStandardMaterial;const name=material.name.replace(/\.\d+$/, '');let geometry=node.geometry.clone();
   if(materials.length>1){const src=geometry.toNonIndexed();const ranges=geometry.groups.filter((g:{materialIndex?:number;start:number;count:number})=>g.materialIndex===i);if(!ranges.length){geometry.dispose();src.dispose();continue}const sliced=new THREE.BufferGeometry();for(const key of Object.keys(src.attributes)){const a=src.attributes[key];const values:number[]=[];for(const range of ranges)for(let j=range.start*a.itemSize;j<(range.start+range.count)*a.itemSize;j++)values.push(a.array[j]);sliced.setAttribute(key,new THREE.Float32BufferAttribute(values,a.itemSize))}geometry.dispose();src.dispose();geometry=sliced;}else if(geometry.index){const nonIndexed=geometry.toNonIndexed();geometry.dispose();geometry=nonIndexed;}
   geometry.clearGroups();geometry.applyMatrix4(node.matrixWorld);
   // The untextured pocket liners include custom meshes without UVs alongside
   // boolean faces that have UVs. Normalize before batching or Three drops the group.
   if(name==='Pocket darkness')for(const attribute of Object.keys(geometry.attributes))if(attribute!=='position'&&attribute!=='normal')geometry.deleteAttribute(attribute);
   if(name==='Cloth'||name==='Cushion'||name.includes('walnut')||name.startsWith('Parquet')){
    const pos=geometry.getAttribute('position'),norm=geometry.getAttribute('normal'),uv=new Float32Array(pos.count*2);
    const textile=name==='Cloth'||name==='Cushion';
    for(let v=0;v<pos.count;v++){
     const nx=Math.abs(norm.getX(v)),ny=Math.abs(norm.getY(v)),nz=Math.abs(norm.getZ(v));
     const u=nx>ny&&nx>nz?pos.getZ(v):pos.getX(v),vv=ny>nz?pos.getZ(v):pos.getY(v);
     uv[v*2]=u/(textile?.18:1.4);uv[v*2+1]=vv/(textile?.18:.24);
    }geometry.setAttribute('uv',new THREE.BufferAttribute(uv,2));
   }
   const key=material.uuid;
   if(!groups.has(key)){
    const m=material.clone();m.name=name;
    if(name==='Cloth'||name==='Cushion'){
     m.color.set(cloth==='blue'?'#175369':'#12563c');m.map=textures.felt;m.bumpMap=textures.felt;m.bumpScale=.00035;m.roughness=.96;m.metalness=0;
     if(name==='Cushion')m.color.multiplyScalar(.90);
    }
    if(name.includes('walnut')){m.map=textures.wood;m.color.set('#d6c8ba');m.roughness=.38;m.metalness=0;m.envMapIntensity=.45;}
    if(name.startsWith('Parquet')){m.map=textures.wood;m.color.set('#655f59');m.roughness=.88;m.envMapIntensity=.15;}
    if(name==='Pocket darkness'){m.color.set('#040605');m.roughness=1;m.metalness=0;m.side=THREE.DoubleSide;}
    if(name==='Brushed antique brass'){m.color.set('#9e8960');m.roughness=.46;}
    if(room&&venue==='loft'){if(name==='Deep green wall')m.color.set('#252d35');if(name==='Wall walnut')m.color.set('#25272b');if(name==='Lounge upholstery')m.color.set('#293e4a');}
    groups.set(key,{geometries:[],material:name==='Pocket darkness'?new THREE.MeshBasicMaterial({color:'#050706',side:THREE.DoubleSide}):m,name});
   }
   groups.get(key)!.geometries.push(geometry);
  }
 });
 const group=new THREE.Group();
 const add=(geometry:THREE.BufferGeometry,material:THREE.Material,name:string)=>{const mesh=new THREE.Mesh(geometry,material);mesh.name=name;mesh.castShadow=!room;mesh.receiveShadow=true;group.add(mesh)};
 for(const {geometries,material,name} of groups.values()){
  const combined=mergeGeometries(geometries);
  if(combined){add(combined,material,name);geometries.forEach(g=>g.dispose())}
  else{
   // Preserve visible geometry if a future asset has an incompatible attribute layout.
   for(const geometry of geometries)add(geometry,material,name);
  }
 }return group;
}
export default function ClubModel({cloth='green',venue='club',hero=false}:{cloth?:string;venue?:string;hero?:boolean}){
 const table=useGLTF('/models/club-table.glb?v=20261108-play6'),club=useGLTF('/models/club-room.glb?v=20261108-play6');
 const textures=useMemo(surfaceTextures,[]);
 const t=useMemo(()=>optimized(table.scene,cloth,venue,false,textures),[table.scene,cloth,venue,textures]);
 const r=useMemo(()=>optimized(club.scene,cloth,venue,true,textures),[club.scene,cloth,venue,textures]);
 useEffect(()=>()=>{for(const group of [t,r])group.traverse(o=>{if(o instanceof THREE.Mesh){o.geometry.dispose();o.material.dispose()}})},[t,r]);
 useEffect(()=>()=>Object.values(textures).forEach(t=>t.dispose()),[textures]);
 return <><primitive object={t}/>{!hero&&<primitive object={r}/>}</>
}
useGLTF.preload('/models/club-table.glb?v=20261108-play6');
