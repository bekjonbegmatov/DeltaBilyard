import {Suspense,useMemo,useRef,useState,useEffect} from 'react';
import {Canvas,useFrame,useThree,ThreeEvent} from '@react-three/fiber';
import {Line,PerspectiveCamera,PerformanceMonitor,OrbitControls,Html} from '@react-three/drei';
import * as THREE from 'three';
import {RoomEnvironment} from 'three/examples/jsm/environments/RoomEnvironment.js';
import type {Room,Ball} from './types';
import {playSound} from './audio';
import ClubModel from './ClubModel';
import {W,H,R,pockets} from './tableGeometry';
import {replayAt} from './replay';
import {aimGuide} from './aimGuide';
import {haptic} from './haptics';
export {pockets} from './tableGeometry';
const colors=['#fff9e7','#eac129','#2775b5','#d3413a','#8b55ad','#ec8735','#248a63','#8f293a','#181c20','#eac129','#2775b5','#d3413a','#8b55ad','#ec8735','#248a63','#8f293a'];
export function rack():Ball[]{const b:Ball[]=[{id:0,x:W*.25,y:H/2,pocketed:false}];let k=0;const ids=[1,9,2,10,8,3,4,11,5,12,6,13,7,14,15];for(let r=0;r<5;r++)for(let c=0;c<=r;c++)b.push({id:ids[k++],x:W*.72+r*R*Math.sqrt(3)*1.005,y:H/2+(2*c-r)*R*1.005,pocketed:false});return b.sort((a,b)=>a.id-b.id)}
function texture(id:number,style='classic'){const c=document.createElement('canvas');c.width=256;c.height=128;const x=c.getContext('2d')!;const ink=style==='black'?'#171d23':style==='ivory'?'#d5c8a7':colors[id];x.fillStyle=id===0?'#fff9e7':style==='classic'&&id>8?'#fff9e7':ink;x.fillRect(0,0,256,128);if(id>8){x.fillStyle=style==='classic'?colors[id]:style==='black'?'#40494f':'#a79b7b';x.fillRect(0,32,256,64)}if(id){for(const at of [64,192]){x.beginPath();x.arc(at,64,20,0,Math.PI*2);x.fillStyle='#fff9e7';x.fill();x.fillStyle='#17211d';x.font='bold 27px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(''+id,at,65)}}const t=new THREE.CanvasTexture(c);t.colorSpace=THREE.SRGBColorSpace;return t}
// A local studio reflection map: no network asset or expensive live cube capture.
function StudioEnvironment(){const {gl,scene}=useThree();useEffect(()=>{const generator=new THREE.PMREMGenerator(gl);const studio=new RoomEnvironment();const target=generator.fromScene(studio,.04);const previous=scene.environment;scene.environment=target.texture;scene.environmentIntensity=.38;studio.dispose();generator.dispose();return()=>{scene.environment=previous;target.dispose()}},[gl,scene]);return null}
function contactTexture(){const canvas=document.createElement('canvas');canvas.width=canvas.height=64;const context=canvas.getContext('2d')!;const gradient=context.createRadialGradient(32,32,4,32,32,32);gradient.addColorStop(0,'rgba(0,0,0,.62)');gradient.addColorStop(.35,'rgba(0,0,0,.35)');gradient.addColorStop(1,'rgba(0,0,0,0)');context.fillStyle=gradient;context.fillRect(0,0,64,64);return new THREE.CanvasTexture(canvas)}
export type CameraMode='top'|'aim'|'free';
type Props={room?:Room;angle?:number;power?:number;mine?:boolean;offset?:number;skipReplay?:boolean;onAim?:(a:number)=>void;onPlace?:(x:number,y:number)=>void;called?:number;onPocket?:(p:number)=>void;hero?:boolean;cameraMode?:CameraMode;cloth?:string;venue?:string;cueStyle?:string;ballStyle?:string};
function Scene(p:Props){const balls=useMemo(()=>rack(),[]),meshes=useRef<(THREE.Mesh|null)[]>([]),shadows=useRef<(THREE.Mesh|null)[]>([]),camera=useRef<THREE.PerspectiveCamera>(null),cue=useRef<THREE.Group>(null),lastT=useRef(-1),lastShot=useRef(0),lastMode=useRef(''),look=useRef(new THREE.Vector3());const textures=useMemo(()=>Array.from({length:16},(_,i)=>texture(i,p.ballStyle)),[p.ballStyle]);const shadowMap=useMemo(contactTexture,[]);const [low,setLow]=useState(false);const mode=p.cameraMode||'top';
 useEffect(()=>()=>{textures.forEach(t=>t.dispose());shadowMap.dispose()},[textures,shadowMap]);
 useFrame(({size},delta)=>{const room=p.room;let positions=(room?.balls||balls).map(b=>[b.x,b.y,b.pocketed?1:0]);let replay=false;let t=0;let clip:ReturnType<typeof replayAt>=null;
  if(room?.shot){const shot=room.shot;const elapsed=(Date.now()+(p.offset||0)-room.shotAt)/1000;clip=!p.skipReplay?replayAt(shot,elapsed):null;replay=!!clip;
   t=clip?clip.t:Math.max(0,Math.min(shot.duration,elapsed));
   const fs=shot.frames;let lo=0,hi=fs.length-1;while(lo+1<hi){const mid=(lo+hi)>>1;if(fs[mid].t<=t)lo=mid;else hi=mid}const a=fs[lo],b=fs[Math.min(lo+1,fs.length-1)],f=Math.max(0,Math.min(1,(t-a.t)/(b.t-a.t||1)));positions=a.balls.map((v,j)=>[THREE.MathUtils.lerp(v[0],b.balls[j][0],f),THREE.MathUtils.lerp(v[1],b.balls[j][1],f),f<1?v[2]:b.balls[j][2]]);
   if(lastShot.current!==room.shotAt){lastShot.current=room.shotAt;lastT.current=-1}if(!replay&&elapsed<=shot.duration)for(const e of shot.events)if(e.t>lastT.current&&e.t<=t&&e.t>=t-.12){playSound(e.type,e.strength);if(e.type==='collision'||e.type==='rail')haptic(e.type,e.strength);if(e.type==='pocket'&&e.ball!==0)haptic('pot');}if(!replay)lastT.current=t;if(elapsed>=shot.duration&&!clip)positions=room.balls.map(b=>[b.x,b.y,b.pocketed?1:0]);
  }
  for(let i=0;i<16;i++){const m=meshes.current[i];if(!m)continue;const b=positions[i];m.visible=b[2]===0;const nx=b[0]-W/2,nz=b[1]-H/2;const dx=nx-m.position.x,dz=nz-m.position.z;m.rotation.x+=dz/R;m.rotation.z-=dx/R;m.position.set(nx,R+.006,nz);const shadow=shadows.current[i];if(shadow){shadow.visible=m.visible;shadow.position.set(nx+.004,.0068,nz+.006)}}
  if(cue.current){const b=positions[0];cue.current.visible=!!room&&room.phase==='AIM'&&!room.ballInHand;cue.current.position.set(b[0]-W/2,R+.009,b[1]-H/2);cue.current.rotation.y=-(p.angle||0);const backX=-Math.cos(p.angle||0),backY=-Math.sin(p.angle||0);const edgeX=Math.abs(backX)<1e-6?Infinity:((backX>0?W:0)-b[0])/backX;const edgeY=Math.abs(backY)<1e-6?Infinity:((backY>0?H:0)-b[1])/backY;cue.current.rotation.z=-Math.max(.045,Math.min(1.10,Math.atan2(.075,Math.min(edgeX,edgeY)+.02)));}
  if(camera.current){const aspect=size.width/size.height;const distance=Math.max(1.69,2.96/aspect)/(2*Math.tan(42*Math.PI/360));let target=new THREE.Vector3(0,distance,distance*.035);let aimAt=new THREE.Vector3(0,0,0);let fov=42;
   if(p.hero){target=new THREE.Vector3(.2,2.45,1.45);fov=aspect>2?29:40;}
   else if(mode==='aim'&&!replay&&room?.phase!=='SHOT'){const b=positions[0],a=p.angle||0;target=new THREE.Vector3(b[0]-W/2-Math.cos(a)*.63,.33,b[1]-H/2-Math.sin(a)*.63);aimAt=new THREE.Vector3(b[0]-W/2+Math.cos(a)*.9,.02,b[1]-H/2+Math.sin(a)*.9);fov=58;}
   else if(mode==='free'&&!replay){if(lastMode.current!=='free'){camera.current.position.set(2.2,1.75,2.1);camera.current.lookAt(0,-.1,0)}lastMode.current='free';return;}
   if(clip){const pt=pockets[clip.event.pocket];const side=clip.index%2===1;aimAt=new THREE.Vector3(pt[0]-W/2,-.015,pt[1]-H/2);target=aimAt.clone().add(new THREE.Vector3(side?.48:-.48,side?.52:.75,pt[1]<H/2?.65:-.65));fov=46;}
   lastMode.current=replay?'replay':mode;camera.current.fov=THREE.MathUtils.lerp(camera.current.fov,fov,Math.min(1,delta*6));camera.current.updateProjectionMatrix();camera.current.position.lerp(target,Math.min(1,delta*7));look.current.lerp(aimAt,Math.min(1,delta*7));camera.current.lookAt(look.current);
  }
 });
 function pointer(e:ThreeEvent<PointerEvent>){if(!p.mine||p.room?.phase!=='AIM'||mode==='free')return;e.stopPropagation();const x=e.point.x+W/2,y=e.point.z+H/2;if(p.room.ballInHand){if(e.type==='pointerdown')p.onPlace?.(x,y)}else{const b=p.room.balls[0];p.onAim?.(Math.atan2(y-b.y,x-b.x));}}
 const a=p.angle||0;
 return <><StudioEnvironment/><PerformanceMonitor onDecline={()=>setLow(true)}/><PerspectiveCamera ref={camera} makeDefault position={[0,2.7,.35]} fov={42} near={.015} far={24}/><ambientLight intensity={.15}/><hemisphereLight args={['#dce6e4','#211c17',.42]}/><directionalLight position={[-.5,3.8,-.2]} intensity={1.25} color="#fff4dc" castShadow shadow-mapSize={[low?512:2048,low?512:2048]} shadow-camera-left={-1.7} shadow-camera-right={1.7} shadow-camera-top={1.15} shadow-camera-bottom={-1.15} shadow-camera-near={.1} shadow-camera-far={8} shadow-bias={-.00008} shadow-normalBias={.001} shadow-radius={3}/><spotLight position={[0,2.3,0]} intensity={8} angle={.8} penumbra={.65} color="#fff4df" distance={6}/><pointLight position={[-2,1.4,-2]} intensity={3.0} color="#dfb77f" distance={5}/><pointLight position={[2,1.3,2.2]} intensity={2} color={p.venue==='loft'?'#8baecd':'#d0b089'} distance={5}/>
 <ClubModel cloth={p.cloth} venue={p.venue} hero={p.hero}/>
 <mesh position={[0,.009,0]} rotation={[-Math.PI/2,0,0]} onPointerDown={pointer} onPointerMove={e=>{if(e.buttons)pointer(e)}}><planeGeometry args={[W,H]}/><meshBasicMaterial transparent opacity={0} depthWrite={false}/></mesh>
 {pockets.map(([x,y],i)=><mesh key={i} position={[x-W/2,.011,y-H/2]} rotation={[-Math.PI/2,0,0]} onClick={e=>{e.stopPropagation();p.onPocket?.(i)}}><ringGeometry args={[.073,.083,40]}/><meshBasicMaterial color="#f0c66b" transparent opacity={p.called===i?.9:0} depthWrite={false}/></mesh>)}
 {balls.map(ball=><mesh key={'shadow-'+ball.id} ref={m=>{shadows.current[ball.id]=m}} position={[ball.x-W/2,.0068,ball.y-H/2]} rotation={[-Math.PI/2,0,0]}><planeGeometry args={[R*3.4,R*3.4]}/><meshBasicMaterial map={shadowMap} transparent depthWrite={false} toneMapped={false}/></mesh>)}
 {balls.map(ball=><mesh key={ball.id} ref={m=>{meshes.current[ball.id]=m}} position={[ball.x-W/2,R+.006,ball.y-H/2]} castShadow><sphereGeometry args={[R,low?16:32,low?12:24]}/><meshPhysicalMaterial map={textures[ball.id]} roughness={.21} metalness={0} clearcoat={.55} clearcoatRoughness={.16} envMapIntensity={.65}/></mesh>)}
 <group ref={cue}><group position={[-.038-(p.power||.3)*.09,0,0]}>
 <mesh renderOrder={20} position={[-.007,0,0]} rotation={[0,0,Math.PI/2]}><cylinderGeometry args={[.006,.006,.014,16]}/><meshStandardMaterial depthTest={false} depthWrite={false} color="#58777b" roughness={.8}/></mesh>
 <mesh renderOrder={20} position={[-.024,0,0]} rotation={[0,0,Math.PI/2]}><cylinderGeometry args={[.006,.0065,.020,16]}/><meshStandardMaterial depthTest={false} depthWrite={false} color="#e5dec5" roughness={.27}/></mesh>
 <mesh renderOrder={20} position={[-.422,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.0065,.010,.776,20]}/><meshStandardMaterial depthTest={false} depthWrite={false} color={p.cueStyle==='carbon'?'#343a3a':'#c9ab77'} roughness={.35}/></mesh>
 <mesh renderOrder={20} position={[-.995,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.010,.014,.37,20]}/><meshStandardMaterial depthTest={false} depthWrite={false} color={p.cueStyle==='carbon'?'#202624':'#4d2f20'} roughness={.30}/></mesh>
 <mesh renderOrder={20} position={[-1.30,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.014,.015,.24,20]}/><meshStandardMaterial depthTest={false} depthWrite={false} color="#202722" roughness={.7}/></mesh>
 <mesh renderOrder={20} position={[-1.435,0,0]} rotation={[0,0,-Math.PI/2]}><cylinderGeometry args={[.015,.015,.03,20]}/><meshStandardMaterial depthTest={false} depthWrite={false} color="#171b19" roughness={.75}/></mesh>
 </group></group>
 {p.mine&&p.room?.hints&&p.room.phase==='AIM'&&!p.room.ballInHand&&mode!=='free'&&<AimHints balls={p.room.balls} angle={a}/>}
 {mode==='free'&&!p.hero&&<OrbitControls makeDefault enableDamping dampingFactor={.08} minDistance={.5} maxDistance={5} minPolarAngle={.08} maxPolarAngle={Math.PI*.47} target={[0,-.05,0]} enablePan={false} touches={{ONE:THREE.TOUCH.ROTATE,TWO:THREE.TOUCH.DOLLY_PAN}}/>}
 </>;
}
function AimHints({balls,angle}:{balls:Ball[];angle:number}){
 const guide=useMemo(()=>aimGuide(balls,angle),[balls,angle]);
 const points=(v:number[][])=>v.map(p=>[p[0]-W/2,R+.007,p[1]-H/2] as [number,number,number]);
 return <>{guide.path.length>1&&<Line points={points(guide.path)} color="#e2ddbf" lineWidth={1.2} dashed dashSize={.018} gapSize={.012}/>}{guide.ghost&&<mesh position={[guide.ghost[0]-W/2,R+.006,guide.ghost[1]-H/2]}><sphereGeometry args={[R,24,16]}/><meshBasicMaterial color="#fff9df" transparent opacity={.18} depthWrite={false}/></mesh>}{guide.objectPath&&<Line points={points(guide.objectPath)} color="#e7c77d" lineWidth={1.6}/>} {guide.cuePath&&<Line points={points(guide.cuePath)} color="#afcdd6" lineWidth={1} dashed dashSize={.012} gapSize={.015}/>}</>
}
export default function Table(p:Props){return <Canvas resize={{scroll:false,debounce:{scroll:0,resize:0}}} shadows dpr={[1,1.75]} gl={{antialias:true,alpha:true,powerPreference:'high-performance',toneMapping:THREE.ACESFilmicToneMapping,toneMappingExposure:.95}}><Suspense fallback={<Html center><div className="scene-loading">Готовим ваш стол…</div></Html>}><Scene {...p}/></Suspense></Canvas>}
