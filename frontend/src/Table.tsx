import {Suspense,useMemo,useRef,useState,useEffect} from 'react';
import {Canvas,useFrame,ThreeEvent} from '@react-three/fiber';
import {Line,PerspectiveCamera,PerformanceMonitor,OrbitControls,Html} from '@react-three/drei';
import * as THREE from 'three';
import type {Room,Ball} from './types';
import {playSound} from './audio';
import ClubModel from './ClubModel';
const W=2.54,H=1.27,R=.028575;
export const pockets=[[0,0],[W/2,0],[W,0],[0,H],[W/2,H],[W,H]];
const colors=['#fff9e7','#eac129','#2775b5','#d3413a','#8b55ad','#ec8735','#248a63','#8f293a','#181c20','#eac129','#2775b5','#d3413a','#8b55ad','#ec8735','#248a63','#8f293a'];
export function rack():Ball[]{const b:Ball[]=[{id:0,x:W*.25,y:H/2,pocketed:false}];let k=0;const ids=[1,9,2,10,8,3,4,11,5,12,6,13,7,14,15];for(let r=0;r<5;r++)for(let c=0;c<=r;c++)b.push({id:ids[k++],x:W*.72+r*R*Math.sqrt(3)*1.005,y:H/2+(2*c-r)*R*1.005,pocketed:false});return b.sort((a,b)=>a.id-b.id)}
function texture(id:number){const c=document.createElement('canvas');c.width=256;c.height=128;const x=c.getContext('2d')!;x.fillStyle=id>8?'#fff9e7':colors[id];x.fillRect(0,0,256,128);if(id>8){x.fillStyle=colors[id];x.fillRect(0,32,256,64)}if(id){for(const at of [64,192]){x.beginPath();x.arc(at,64,20,0,Math.PI*2);x.fillStyle='#fff9e7';x.fill();x.fillStyle='#17211d';x.font='bold 27px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(''+id,at,65)}}const t=new THREE.CanvasTexture(c);t.colorSpace=THREE.SRGBColorSpace;return t}
export type CameraMode='top'|'aim'|'free';
type Props={room?:Room;angle?:number;power?:number;mine?:boolean;offset?:number;skipReplay?:boolean;onAim?:(a:number)=>void;onPlace?:(x:number,y:number)=>void;called?:number;onPocket?:(p:number)=>void;hero?:boolean;cameraMode?:CameraMode;cloth?:string;venue?:string;cueStyle?:string};
function Scene(p:Props){const balls=useMemo(()=>rack(),[]),meshes=useRef<(THREE.Mesh|null)[]>([]),camera=useRef<THREE.PerspectiveCamera>(null),cue=useRef<THREE.Group>(null),lastT=useRef(-1),lastShot=useRef(0),lastMode=useRef(''),look=useRef(new THREE.Vector3());const textures=useMemo(()=>Array.from({length:16},(_,i)=>texture(i)),[]);const [low,setLow]=useState(false);const mode=p.cameraMode||'top';
 useEffect(()=>()=>textures.forEach(t=>t.dispose()),[textures]);
 useFrame(({size},delta)=>{const room=p.room;let positions=(room?.balls||balls).map(b=>[b.x,b.y,b.pocketed?1:0]);let replay=false;let t=0;
  if(room?.shot){const shot=room.shot;const elapsed=(Date.now()+(p.offset||0)-room.shotAt)/1000;replay=elapsed>shot.duration&&!p.skipReplay;
   t=replay?Math.max(0,shot.duration-2)+((elapsed-shot.duration)*.65)%Math.min(2,shot.duration):Math.max(0,Math.min(shot.duration,elapsed));
   const fs=shot.frames;let lo=0,hi=fs.length-1;while(lo+1<hi){const mid=(lo+hi)>>1;if(fs[mid].t<=t)lo=mid;else hi=mid}const a=fs[lo],b=fs[Math.min(lo+1,fs.length-1)],f=Math.max(0,Math.min(1,(t-a.t)/(b.t-a.t||1)));positions=a.balls.map((v,j)=>[THREE.MathUtils.lerp(v[0],b.balls[j][0],f),THREE.MathUtils.lerp(v[1],b.balls[j][1],f),f<1?v[2]:b.balls[j][2]]);
   if(lastShot.current!==room.shotAt){lastShot.current=room.shotAt;lastT.current=-1}if(!replay)for(const e of shot.events)if(e.t>lastT.current&&e.t<=t)playSound(e.type,e.strength);lastT.current=t;
  }
  for(let i=0;i<16;i++){const m=meshes.current[i];if(!m)continue;const b=positions[i];m.visible=b[2]===0;const nx=b[0]-W/2,nz=b[1]-H/2;const dx=nx-m.position.x,dz=nz-m.position.z;m.rotation.x+=dz/R;m.rotation.z-=dx/R;m.position.set(nx,R+.006,nz)}
  if(cue.current){const b=positions[0];cue.current.visible=!!room&&room.phase==='AIM'&&!room.ballInHand;cue.current.position.set(b[0]-W/2,R+.009,b[1]-H/2);cue.current.rotation.y=-(p.angle||0);}
  if(camera.current){const aspect=size.width/size.height;const distance=Math.max(1.98,3.45/aspect)/(2*Math.tan(44*Math.PI/360));let target=new THREE.Vector3(0,distance,.001);let aimAt=new THREE.Vector3(0,0,0);let fov=44;
   if(p.hero){target=new THREE.Vector3(.2,2.45,1.45);fov=aspect>2?29:40;}
   else if(mode==='aim'&&!replay&&room?.phase!=='SHOT'){const b=positions[0],a=p.angle||0;target=new THREE.Vector3(b[0]-W/2-Math.cos(a)*.63,.33,b[1]-H/2-Math.sin(a)*.63);aimAt=new THREE.Vector3(b[0]-W/2+Math.cos(a)*.9,.02,b[1]-H/2+Math.sin(a)*.9);fov=58;}
   else if(mode==='free'&&!replay){if(lastMode.current!=='free'){camera.current.position.set(2.2,1.75,2.1);camera.current.lookAt(0,-.1,0)}lastMode.current='free';return;}
   if(replay&&room?.shot){const pot=room.shot.events.find(e=>e.type==='pocket'&&e.ball!==0);if(pot){const pt=pockets[pot.pocket];const side=((Date.now()+(p.offset||0)-room.shotAt)/1000-room.shot.duration)>1.7;aimAt=new THREE.Vector3(pt[0]-W/2,0,pt[1]-H/2);target=aimAt.clone().add(new THREE.Vector3(side?.4:-.4,.55,.7));fov=48;}}
   lastMode.current=replay?'replay':mode;camera.current.fov=THREE.MathUtils.lerp(camera.current.fov,fov,Math.min(1,delta*6));camera.current.updateProjectionMatrix();camera.current.position.lerp(target,Math.min(1,delta*7));look.current.lerp(aimAt,Math.min(1,delta*7));camera.current.lookAt(look.current);
  }
 });
 function pointer(e:ThreeEvent<PointerEvent>){if(!p.mine||p.room?.phase!=='AIM'||mode==='free')return;e.stopPropagation();const x=e.point.x+W/2,y=e.point.z+H/2;if(p.room.ballInHand){if(e.type==='pointerdown')p.onPlace?.(x,y)}else{const b=p.room.balls[0];p.onAim?.(Math.atan2(y-b.y,x-b.x));}}
 const b=p.room?.balls[0],a=p.angle||0;const start:[number,number,number]=[b?b.x-W/2:0,.04,b?b.y-H/2:0];
 return <><PerformanceMonitor onDecline={()=>setLow(true)}/><PerspectiveCamera ref={camera} makeDefault position={[0,2.7,.001]} fov={44} near={.015} far={24}/><ambientLight intensity={.65}/><hemisphereLight args={['#e4eee1','#4c3521',1.1]}/><directionalLight position={[.8,4,1]} intensity={2.8} castShadow shadow-mapSize={[low?512:1024,low?512:1024]} shadow-camera-left={-2} shadow-camera-right={2} shadow-camera-top={2} shadow-camera-bottom={-2} shadow-bias={-.0002}/><pointLight position={[-2,1.2,-2]} intensity={14} color="#e7b779" distance={7}/><pointLight position={[2,1,2.2]} intensity={9} color={p.venue==='loft'?'#8baecd':'#e7bb89'} distance={6}/>
 <ClubModel cloth={p.cloth} venue={p.venue} hero={p.hero}/>
 <mesh position={[0,.009,0]} rotation={[-Math.PI/2,0,0]} onPointerDown={pointer} onPointerMove={e=>{if(e.buttons)pointer(e)}}><planeGeometry args={[W,H]}/><meshBasicMaterial transparent opacity={0} depthWrite={false}/></mesh>
 {pockets.map(([x,y],i)=><mesh key={i} position={[x-W/2,.011,y-H/2]} rotation={[-Math.PI/2,0,0]} onClick={e=>{e.stopPropagation();p.onPocket?.(i)}}><ringGeometry args={[.073,.083,40]}/><meshBasicMaterial color="#f0c66b" transparent opacity={p.called===i?.9:0} depthWrite={false}/></mesh>)}
 {balls.map(ball=><mesh key={ball.id} ref={m=>{meshes.current[ball.id]=m}} position={[ball.x-W/2,R+.006,ball.y-H/2]} castShadow><sphereGeometry args={[R,low?16:32,low?12:24]}/><meshStandardMaterial map={textures[ball.id]} roughness={.16} metalness={.02}/></mesh>)}
 <group ref={cue}><mesh position={[-.44-(p.power||.3)*.10,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.005,.012,.72,16]}/><meshStandardMaterial color={p.cueStyle==='carbon'?'#24292b':'#c8a267'} roughness={.35}/></mesh><mesh position={[-.08-(p.power||.3)*.10,0,0]} rotation={[0,0,Math.PI/2]}><cylinderGeometry args={[.005,.005,.014,16]}/><meshStandardMaterial color="#6bb0b5"/></mesh><mesh position={[-.68-(p.power||.3)*.10,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.010,.012,.22,16]}/><meshStandardMaterial color="#1c2220" roughness={.75}/></mesh></group>
 {p.mine&&p.room?.hints&&p.room.phase==='AIM'&&!p.room.ballInHand&&mode!=='free'&&<Line points={[start,[start[0]+Math.cos(a)*.65,.04,start[2]+Math.sin(a)*.65]]} color="#e5dcb8" lineWidth={1.1} dashed dashSize={.025} gapSize={.012}/>}
 {mode==='free'&&!p.hero&&<OrbitControls makeDefault enableDamping dampingFactor={.08} minDistance={.5} maxDistance={5} minPolarAngle={.08} maxPolarAngle={Math.PI*.47} target={[0,-.05,0]} enablePan={false} touches={{ONE:THREE.TOUCH.ROTATE,TWO:THREE.TOUCH.DOLLY_PAN}}/>}
 </>;
}
export default function Table(p:Props){return <Canvas shadows dpr={[1,1.75]} gl={{antialias:true,alpha:true,powerPreference:'high-performance'}}><Suspense fallback={<Html center><div className="scene-loading">Готовим ваш стол…</div></Html>}><Scene {...p}/></Suspense></Canvas>}
