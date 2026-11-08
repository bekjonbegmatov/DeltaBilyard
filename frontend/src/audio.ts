const assets={
 collision:'ball-contact.wav',cue:'cue-strike.wav',rail:'rail-contact.wav',pocket:'pocket-drop.wav',
 laugh:'laughter.wav',clap:'hand-clap.wav',wow:'wow.wav',cheer:'cheer.wav',
} as const;
type Sample=keyof typeof assets;
type Voice={source:AudioBufferSourceNode;gain:GainNode;reaction:boolean};
const buffers=new Map<Sample,AudioBuffer>();
const pending=new Map<Sample,Promise<void>>();
const failedAt=new Map<Sample,number>();
const voices=new Set<Voice>();
const lastImpact=new Map<Sample,number>();
let ctx:AudioContext|undefined,master:AudioNode|undefined,reactionAt=-Infinity,variation=0;
export const sound={collision:.5,rail:.4,pocket:.7,reaction:.55};

function context(){
 if(ctx)return ctx;
 if(typeof window==='undefined')return;
 try{
  const Audio=window.AudioContext||(window as typeof window&{webkitAudioContext?:typeof AudioContext}).webkitAudioContext;
  if(!Audio)return;
  ctx=new Audio({latencyHint:'interactive'});
  // Keep a break's many transients from clipping, without changing voice-chat volume.
  const limiter=ctx.createDynamicsCompressor();
  limiter.threshold.value=-9;limiter.knee.value=6;limiter.ratio.value=8;
  limiter.attack.value=.002;limiter.release.value=.08;
  limiter.connect(ctx.destination);master=limiter;
  return ctx;
 }catch{ctx=undefined;master=undefined;return}
}

function load(sample:Sample,audio:AudioContext):Promise<void>{
 if(buffers.has(sample))return Promise.resolve();
 const existing=pending.get(sample);if(existing)return existing;
 if(Date.now()-(failedAt.get(sample)??-Infinity)<5000)return Promise.resolve();
 const request=(async()=>{
  try{
   const response=await fetch(`/audio/${assets[sample]}`,{cache:'force-cache'});
   if(!response.ok)throw new Error('Audio asset unavailable');
   const buffer=await audio.decodeAudioData(await response.arrayBuffer());
   buffers.set(sample,buffer);failedAt.delete(sample);
  }catch{failedAt.set(sample,Date.now())}
  finally{pending.delete(sample)}
 })();
 pending.set(sample,request);return request;
}

/** Local recordings only. Preloading never asks for microphone permission or starts playback. */
export async function preloadAudio(){
 const audio=context();if(!audio)return;
 await Promise.all((Object.keys(assets) as Sample[]).map(sample=>load(sample,audio)));
}

/** Call from a pointer/key gesture; Safari may suspend audio again after backgrounding. */
export function unlockAudio(){
 const audio=context();if(!audio)return;
 if(audio.state!=='running')void audio.resume().catch(()=>{});
 void preloadAudio();
}

function volume(value:number){return Number.isFinite(value)?Math.min(1,Math.max(0,value)):0}
function play(sample:Sample,level:number,{after=0,rate=1,reaction=false}={}){
 const audio=ctx,buffer=buffers.get(sample);
 // Never queue an old impact to play after a slow download or a suspended tab wakes up.
 if(!audio||audio.state!=='running'||!buffer||!master||level<=0)return;
 if([...voices].filter(voice=>voice.reaction===reaction).length>=(reaction?6:10))return;
 const source=audio.createBufferSource(),gain=audio.createGain();
 source.buffer=buffer;source.playbackRate.value=rate;gain.gain.value=volume(level);
 source.connect(gain).connect(master);
 const voice={source,gain,reaction};voices.add(voice);
 source.onended=()=>{voices.delete(voice);source.disconnect();gain.disconnect()};
 source.start(audio.currentTime+after);
}

/** Strength is the server's impact speed in m/s; cue/shot use the same scale. */
export function playSound(type:string,strength:number){
 const sample=type==='shot'?'cue':type;
 if(sample!=='collision'&&sample!=='rail'&&sample!=='pocket'&&sample!=='cue')return;
 const audio=ctx;if(!audio||audio.state!=='running'||!Number.isFinite(strength)||strength<0)return;
 if(audio.currentTime-(lastImpact.get(sample)??-Infinity)<.022)return;
 const control=sample==='pocket'?sound.pocket:sample==='rail'?sound.rail:sound.collision;
 if(volume(control)===0)return;
 lastImpact.set(sample,audio.currentTime);
 const intensity=sample==='pocket'?.8:Math.min(1,Math.max(.08,Math.sqrt(strength/6)));
 const rate=[1,.98,1.02,.99,1.01][variation++%5];
 play(sample,intensity*volume(control),{rate});
}

/** Recorded laughter, hand claps, cheering and "wow"; see /audio/CREDITS.md. */
export function playReaction(emoji:string){
 const audio=ctx;if(!audio||audio.state!=='running'||volume(sound.reaction)===0||audio.currentTime-reactionAt<.4)return;
 if(!['😂','👏','🔥','🎯','😮','🤝','😎'].includes(emoji))return;
 reactionAt=audio.currentTime;
 // A new reaction replaces the previous one instead of building a wall of voices.
 for(const voice of [...voices])if(voice.reaction){try{voice.source.stop()}catch{/* already ended */}voices.delete(voice)}
 const level=volume(sound.reaction)*.65;
 if(emoji==='😂')play('laugh',level,{reaction:true});
 else if(emoji==='😮')play('wow',level,{reaction:true});
 else if(emoji==='👏'||emoji==='🤝'){
  const count=emoji==='🤝'?2:5;
  for(let i=0;i<count;i++)play('clap',level*(i%2?.85:1),{after:i*.16,rate:i%2?.96:1.02,reaction:true});
 }else play('cheer',level*.8,{reaction:true});
}
