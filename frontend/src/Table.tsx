import {Suspense,useMemo,useRef,useState} from 'react';
import {Canvas,useFrame,ThreeEvent} from '@react-three/fiber';
import {Line,PerspectiveCamera,PerformanceMonitor} from '@react-three/drei';
import * as THREE from 'three';
import type {Room,Ball} from './types';
import {playSound} from './audio';
const W=2.54,H=1.27,R=.028575;
export const pockets=[[0,0],[W/2,0],[W,0],[0,H],[W/2,H],[W,H]];
const colors=['#fff9e7','#eac129','#2775b5','#d3413a','#8b55ad','#ec8735','#248a63','#8f293a','#181c20','#eac129','#2775b5','#d3413a','#8b55ad','#ec8735','#248a63','#8f293a'];
export function rack():Ball[]{const b:Ball[]=[{id:0,x:W*.25,y:H/2,pocketed:false}];let k=0;const ids=[1,9,2,10,8,3,4,11,5,12,6,13,7,14,15];for(let r=0;r<5;r++)for(let c=0;c<=r;c++)b.push({id:ids[k++],x:W*.72+r*R*Math.sqrt(3)*1.005,y:H/2+(2*c-r)*R*1.005,pocketed:false});return b.sort((a,b)=>a.id-b.id)}
function texture(id:number){const c=document.createElement('canvas');c.width=256;c.height=128;const x=c.getContext('2d')!;x.fillStyle=id>8?'#fff9e7':colors[id];x.fillRect(0,0,256,128);if(id>8){x.fillStyle=colors[id];x.fillRect(0,32,256,64)}if(id){for(const at of [64,192]){x.beginPath();x.arc(at,64,20,0,Math.PI*2);x.fillStyle='#fff9e7';x.fill();x.fillStyle='#17211d';x.font='bold 27px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(''+id,at,65)}}const t=new THREE.CanvasTexture(c);t.colorSpace=THREE.SRGBColorSpace;return t}
type Props={room?:Room;angle?:number;power?:number;mine?:boolean;offset?:number;skipReplay?:boolean;onAim?:(a:number)=>void;onPlace?:(x:number,y:number)=>void;called?:number;onPocket?:(p:number)=>void;hero?:boolean};
function Scene(p:Props){const balls=useMemo(()=>rack(),[]),meshes=useRef<(THREE.Mesh|null)[]>([]),camera=useRef<THREE.PerspectiveCamera>(null),cue=useRef<THREE.Group>(null),lastT=useRef(-1),lastShot=useRef(0);const textures=useMemo(()=>Array.from({length:16},(_,i)=>texture(i)),[]);const [low,setLow]=useState(false);
 useFrame(({clock,size},delta)=>{const room=p.room;let positions=(room?.balls||balls).map(b=>[b.x,b.y,b.pocketed?1:0]);let replay=false;let t=0;
  if(room?.shot){const shot=room.shot;const elapsed=(Date.now()+(p.offset||0)-room.shotAt)/1000;replay=elapsed>shot.duration&&!p.skipReplay;
   t=replay?Math.max(0,shot.duration-2)+((elapsed-shot.duration)*.65)%Math.min(2,shot.duration):Math.max(0,Math.min(shot.duration,elapsed));
   const fs=shot.frames;let i=0;while(i<fs.length-2&&fs[i+1].t<t)i++;const a=fs[i],b=fs[Math.min(i+1,fs.length-1)],f=Math.max(0,Math.min(1,(t-a.t)/(b.t-a.t||1)));positions=a.balls.map((v,j)=>[THREE.MathUtils.lerp(v[0],b.balls[j][0],f),THREE.MathUtils.lerp(v[1],b.balls[j][1],f),f<1?v[2]:b.balls[j][2]]);
   if(lastShot.current!==room.shotAt){lastShot.current=room.shotAt;lastT.current=-1}if(!replay)for(const e of shot.events)if(e.t>lastT.current&&e.t<=t)playSound(e.type,e.strength);lastT.current=t;
  }
  for(let i=0;i<16;i++){const m=meshes.current[i];if(!m)continue;const b=positions[i];m.visible=b[2]===0;const nx=b[0]-W/2,nz=b[1]-H/2;const dx=nx-m.position.x,dz=nz-m.position.z;m.rotation.x+=dz/R;m.rotation.z-=dx/R;m.position.set(nx,R+.012,nz)}
  if(cue.current){const b=positions[0];cue.current.visible=!!room&&room.phase==='AIM'&&!room.ballInHand;cue.current.position.set(b[0]-W/2,R+.015,b[1]-H/2);cue.current.rotation.y=-(p.angle||0);}
  if(camera.current){const aspect=size.width/size.height;const distance=aspect<1.5?3.5:2.7;let target=new THREE.Vector3(0,distance,room?.phase==='SHOT'&&replay ? .8 : .01);
   if(p.hero){target=new THREE.Vector3(.1,2.7,1.3);camera.current.fov=aspect>2?28:40;camera.current.updateProjectionMatrix();}
   if(replay&&room?.shot){const pot=room.shot.events.find(e=>e.type==='pocket'&&e.ball!==0);if(pot){const pt=pockets[pot.pocket];const side=((Date.now()+(p.offset||0)-room.shotAt)/1000-room.shot.duration)>1.7;target=new THREE.Vector3((pt[0]-W/2)*.65+(side?.5:-.3),1.1,(pt[1]-H/2)*.6+.6);}}
   camera.current.position.lerp(target,Math.min(1,delta*5));camera.current.lookAt(0,0,0);
  }
 });
 function pointer(e:ThreeEvent<PointerEvent>){if(!p.mine||p.room?.phase!=='AIM')return;e.stopPropagation();const x=e.point.x+W/2,y=e.point.z+H/2;if(p.room.ballInHand){if(e.type==='pointerdown')p.onPlace?.(x,y)}else{const b=p.room.balls[0];p.onAim?.(Math.atan2(y-b.y,x-b.x));}}
 const b=p.room?.balls[0];const a=p.angle||0;const start:[number,number,number]=[b?b.x-W/2:0,.045,b?b.y-H/2:0];
 return <><PerformanceMonitor onDecline={()=>setLow(true)}/><PerspectiveCamera ref={camera} makeDefault position={[0,2.7,.01]} fov={44} near={.01} far={30}/><ambientLight intensity={.65}/><hemisphereLight args={['#e1f3e6','#283229',1.4]}/><directionalLight position={[1,4,2]} intensity={2} castShadow shadow-mapSize={[low?512:1024,low?512:1024]}/><pointLight position={[-2,1,-1]} intensity={8} color="#e7c484"/>
 <mesh position={[0,-.12,0]} castShadow><boxGeometry args={[2.96,.22,1.69]}/><meshStandardMaterial color="#35241c" roughness={.35}/></mesh><mesh position={[0,-.005,0]} receiveShadow onPointerDown={pointer} onPointerMove={e=>{if(e.buttons)pointer(e)}}><boxGeometry args={[W,.025,H]}/><meshStandardMaterial color="#14755c" roughness={.92}/></mesh>
 {[[-W/2-.055,0,.11,H+.2],[W/2+.055,0,.11,H+.2],[0,-H/2-.055,W,.11],[0,H/2+.055,W,.11]].map(([x,z,w,h],i)=><mesh key={i} position={[x,.025,z]} castShadow><boxGeometry args={[w,.075,h]}/><meshStandardMaterial color="#174f40" roughness={.75}/></mesh>)}
 {pockets.map(([x,y],i)=><group key={i} position={[x-W/2,.032,y-H/2]}><mesh rotation={[-Math.PI/2,0,0]} onClick={e=>{e.stopPropagation();p.onPocket?.(i)}}><circleGeometry args={[.068,32]}/><meshStandardMaterial color={p.called===i?'#d4b675':'#050c09'}/></mesh><mesh rotation={[-Math.PI/2,0,0]}><ringGeometry args={[.067,.076,32]}/><meshStandardMaterial color="#9c855e" metalness={.6} roughness={.5}/></mesh></group>)}
 {[-.9,-.6,-.3,0,.3,.6,.9].flatMap((x,i)=>[-1,1].map(s=><mesh key={i+'-'+s} position={[x,.069,s*.72]} rotation={[-Math.PI/2,0,Math.PI/4]}><planeGeometry args={[.014,.014]}/><meshStandardMaterial color="#e2d3ae"/></mesh>))}
 {balls.map(ball=><mesh key={ball.id} ref={m=>{meshes.current[ball.id]=m}} position={[ball.x-W/2,R+.012,ball.y-H/2]} castShadow><sphereGeometry args={[R,low?16:32,low?12:24]}/><meshStandardMaterial map={textures[ball.id]} roughness={.2} metalness={.03}/></mesh>)}
 <group ref={cue}><mesh position={[-.43-(p.power||.3)*.08,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.006,.012,.7,12]}/><meshStandardMaterial color="#dabd86" roughness={.4}/></mesh><mesh position={[-.085-(p.power||.3)*.08,0,0]} rotation={[0,0,Math.PI/2]}><cylinderGeometry args={[.006,.006,.015,12]}/><meshStandardMaterial color="#61b6ba"/></mesh></group>
 {p.mine&&p.room?.hints&&p.room.phase==='AIM'&&!p.room.ballInHand&&<Line points={[start,[start[0]+Math.cos(a)*.55,.045,start[2]+Math.sin(a)*.55]]} color="#e4d9b0" lineWidth={1.4} dashed dashSize={.022} gapSize={.014}/>}
 {!p.hero&&<mesh position={[0,-.25,0]} rotation={[-Math.PI/2,0,0]} receiveShadow><planeGeometry args={[20,20]}/><meshStandardMaterial color="#111c17" roughness={1}/></mesh>}</>;
}
export default function Table(p:Props){return <Canvas shadows dpr={[1,1.5]} gl={{antialias:true,alpha:true,powerPreference:'high-performance'}}><Suspense fallback={null}><Scene {...p}/></Suspense></Canvas>}
