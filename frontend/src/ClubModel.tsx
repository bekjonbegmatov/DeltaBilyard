import {useMemo} from 'react';
import {useGLTF} from '@react-three/drei';
import * as THREE from 'three';
import {mergeGeometries} from 'three/examples/jsm/utils/BufferGeometryUtils.js';
function optimized(source:THREE.Object3D,cloth:string,venue:string,room:boolean){
 const groups=new Map<string,{geometries:THREE.BufferGeometry[];material:THREE.Material;name:string}>();source.updateMatrixWorld(true);
 source.traverse(node=>{if(!(node instanceof THREE.Mesh))return;if(node.name.startsWith('Pendant'))return;
  const materials=Array.isArray(node.material)?node.material:[node.material];
  // Split multi-material booleans by draw group before static batching.
  for(let i=0;i<materials.length;i++){const material=materials[i] as THREE.MeshStandardMaterial;let geometry=node.geometry.clone();
   if(materials.length>1){const src=geometry.toNonIndexed();const ranges=geometry.groups.filter((g:{materialIndex?:number;start:number;count:number})=>g.materialIndex===i);if(!ranges.length){geometry.dispose();src.dispose();continue}const sliced=new THREE.BufferGeometry();for(const key of Object.keys(src.attributes)){const a=src.attributes[key];const values:number[]=[];for(const range of ranges)for(let j=range.start*a.itemSize;j<(range.start+range.count)*a.itemSize;j++)values.push(a.array[j]);sliced.setAttribute(key,new THREE.Float32BufferAttribute(values,a.itemSize))}geometry.dispose();src.dispose();geometry=sliced;}else if(geometry.index){const nonIndexed=geometry.toNonIndexed();geometry.dispose();geometry=nonIndexed;}
   geometry.clearGroups();geometry.applyMatrix4(node.matrixWorld);const key=material.uuid;
   if(!groups.has(key)){const m=material.clone();if(m.name==='Cloth')m.color.set(cloth==='blue'?'#12516d':'#155b3a');if(m.name==='Cushion')m.color.set(cloth==='blue'?'#0e3c53':'#123f2a');if(room&&venue==='loft'){if(m.name==='Deep green wall')m.color.set('#252d35');if(m.name==='Wall walnut')m.color.set('#25272b');if(m.name==='Lounge upholstery')m.color.set('#293e4a');}groups.set(key,{geometries:[],material:m,name:m.name});}
   groups.get(key)!.geometries.push(geometry);
  }
 });
 const group=new THREE.Group();for(const {geometries,material,name} of groups.values()){const combined=mergeGeometries(geometries);if(!combined)continue;const mesh=new THREE.Mesh(combined,material);mesh.name=name;mesh.castShadow=!room;mesh.receiveShadow=true;group.add(mesh);geometries.forEach(g=>g.dispose())}return group;
}
export default function ClubModel({cloth='green',venue='club',hero=false}:{cloth?:string;venue?:string;hero?:boolean}){const table=useGLTF('/models/club-table.glb'),club=useGLTF('/models/club-room.glb');const t=useMemo(()=>optimized(table.scene,cloth,venue,false),[table.scene,cloth,venue]);const r=useMemo(()=>optimized(club.scene,cloth,venue,true),[club.scene,cloth,venue]);return <><primitive object={t}/>{!hero&&<primitive object={r}/>}</>}
useGLTF.preload('/models/club-table.glb');
