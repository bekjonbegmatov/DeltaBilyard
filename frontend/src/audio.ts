let ctx:AudioContext|undefined;
let reactionAt=-Infinity;
export const sound={collision:.5,rail:.4,pocket:.7,reaction:.55};
export function unlockAudio(){
 try{
  const Audio=window.AudioContext||(window as typeof window&{webkitAudioContext?:typeof AudioContext}).webkitAudioContext;
  if(!Audio)return;
  ctx??=new Audio();
  if(ctx.state==='suspended')void ctx.resume().catch(()=>{});
 }catch{/* Audio unavailable must never prevent a shot. */}
}
export function playSound(type:string,strength:number){
 if(!ctx||ctx.state!=='running')return;
 const gain=ctx.createGain(),osc=ctx.createOscillator();
 const level=type==='pocket'?sound.pocket:type==='rail'?sound.rail:sound.collision;
 gain.gain.setValueAtTime(Math.min(.3,Math.max(.025,strength*.04))*level,ctx.currentTime);
 gain.gain.exponentialRampToValueAtTime(.001,ctx.currentTime+.12);
 osc.type=type==='pocket'?'sine':'triangle';osc.frequency.setValueAtTime(type==='pocket'?180:type==='rail'?280:1400,ctx.currentTime);
 osc.frequency.exponentialRampToValueAtTime(80,ctx.currentTime+.1);
 osc.connect(gain).connect(ctx.destination);osc.onended=()=>{osc.disconnect();gain.disconnect()};osc.start();osc.stop(ctx.currentTime+.13);
}

/** Short local sound effects. No recording, microphone capture or remote asset is used. */
export function playReaction(emoji:string){
 if(!ctx||ctx.state!=='running'||sound.reaction<=0||ctx.currentTime-reactionAt<.25)return;
 reactionAt=ctx.currentTime;
 const context=ctx,start=context.currentTime,volume=Math.max(0,Math.min(1,sound.reaction));
 function tone(at:number,duration:number,from:number,to:number,level:number,type:OscillatorType='sine'){
  const oscillator=context.createOscillator(),gain=context.createGain();
  oscillator.type=type;oscillator.frequency.setValueAtTime(from,start+at);oscillator.frequency.exponentialRampToValueAtTime(Math.max(20,to),start+at+duration);
  gain.gain.setValueAtTime(.0001,start+at);gain.gain.exponentialRampToValueAtTime(Math.max(.0001,level*volume),start+at+.012);gain.gain.exponentialRampToValueAtTime(.0001,start+at+duration);
  oscillator.connect(gain).connect(context.destination);oscillator.onended=()=>{oscillator.disconnect();gain.disconnect()};oscillator.start(start+at);oscillator.stop(start+at+duration+.01);
 }
 function noise(at:number,duration:number,frequency:number,level:number){
  const source=context.createBufferSource(),filter=context.createBiquadFilter(),gain=context.createGain();
  const buffer=context.createBuffer(1,Math.ceil(context.sampleRate*duration),context.sampleRate),samples=buffer.getChannelData(0);
  for(let i=0;i<samples.length;i++)samples[i]=(Math.random()*2-1);
  source.buffer=buffer;filter.type='bandpass';filter.frequency.value=frequency;filter.Q.value=.6;
  gain.gain.setValueAtTime(level*volume,start+at);gain.gain.exponentialRampToValueAtTime(.0001,start+at+duration);
  source.connect(filter).connect(gain).connect(context.destination);source.onended=()=>{source.disconnect();filter.disconnect();gain.disconnect()};source.start(start+at);source.stop(start+at+duration);
 }
 if(emoji==='😂'){
  // A voiced, breathy "ha-ha-ha" effect; deliberately synthetic, not a person's voice.
  for(let i=0;i<4;i++){
   const at=i*.18,frequency=220-i*11;
   tone(at,.145,frequency,frequency*.70,.14,'sawtooth');
   tone(at,.12,frequency*2.9,frequency*2.4,.045,'sine');
   noise(at,.045,1200,.055);
  }
 }else if(emoji==='👏'){
  for(let i=0;i<6;i++){noise(i*.105,.115,1800+(i%2)*600,.22);tone(i*.105,.035,180,110,.035,'triangle')}
 }else if(emoji==='🔥'){
  tone(0,.35,170,630,.14,'triangle');noise(.03,.4,700,.09);tone(.26,.18,700,950,.07);
 }else if(emoji==='🎯'){
  tone(0,.12,660,660,.14);tone(.11,.14,880,880,.14);tone(.23,.29,1320,1100,.13);
 }else if(emoji==='😮'){
  tone(0,.38,220,420,.12,'triangle');tone(.04,.4,450,780,.045);noise(0,.18,900,.03);
 }else if(emoji==='🤝'){
  tone(0,.23,392,392,.11);tone(.13,.25,494,494,.10);tone(.24,.32,587,587,.10);
 }else{
  tone(0,.18,440,523,.12,'triangle');tone(.15,.28,659,587,.10,'triangle');
 }
}
