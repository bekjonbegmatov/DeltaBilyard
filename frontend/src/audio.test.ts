import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';

class Node {
 connect=vi.fn(()=>this);disconnect=vi.fn();
 gain={value:0};threshold={value:0};knee={value:0};ratio={value:0};attack={value:0};release={value:0};
}
class Source extends Node {
 buffer:{sample:string}|null=null;playbackRate={value:1};onended?:()=>void;
 start=vi.fn();stop=vi.fn(()=>this.onended?.());
}
class Audio {
 state='suspended';currentTime=0;destination=new Node();sources:Source[]=[];
 resume=vi.fn(async()=>{this.state='running'});
 createDynamicsCompressor=()=>new Node();createGain=()=>new Node();
 createBufferSource=()=>{const source=new Source();this.sources.push(source);return source};
 decodeAudioData=async(data:ArrayBuffer)=>({sample:new TextDecoder().decode(data)});
 constructor(){instances.push(this)}
}
let instances:Audio[]=[];
beforeEach(()=>{
 vi.resetModules();vi.useFakeTimers();vi.setSystemTime(10000);instances=[];
 vi.stubGlobal('window',{AudioContext:Audio});
 vi.stubGlobal('fetch',vi.fn(async(url:string)=>({ok:true,arrayBuffer:async()=>new TextEncoder().encode(url).buffer})));
});
afterEach(()=>{vi.unstubAllGlobals();vi.useRealTimers()});

describe('recorded audio lifecycle',()=>{
 it('preloads only local recordings without starting or resuming audio',async()=>{
  const {preloadAudio}=await import('./audio');await preloadAudio();
  expect(fetch).toHaveBeenCalledTimes(8);
  expect(vi.mocked(fetch).mock.calls.every(([url])=>String(url).startsWith('/audio/'))).toBe(true);
  expect(instances[0].resume).not.toHaveBeenCalled();expect(instances[0].sources).toHaveLength(0);
 });
 it('requires gesture unlock and then selects the dedicated cue recording',async()=>{
  const {preloadAudio,unlockAudio,playSound}=await import('./audio');await preloadAudio();
  playSound('cue',4);expect(instances[0].sources).toHaveLength(0);
  unlockAudio();playSound('cue',4);
  expect(instances[0].sources[0].buffer?.sample).toBe('/audio/cue-strike.wav');
 });
 it('retries failed loads without playing stale impacts when the files arrive',async()=>{
  vi.mocked(fetch).mockRejectedValue(new Error('offline'));
  const {preloadAudio,unlockAudio,playSound}=await import('./audio');await preloadAudio();unlockAudio();
  playSound('collision',3);expect(instances[0].sources).toHaveLength(0);
  await preloadAudio();expect(fetch).toHaveBeenCalledTimes(8);
  vi.setSystemTime(16000);
  vi.mocked(fetch).mockImplementation(async(url)=>({ok:true,arrayBuffer:async()=>new TextEncoder().encode(String(url)).buffer}) as Response);
  await preloadAudio();expect(fetch).toHaveBeenCalledTimes(16);expect(instances[0].sources).toHaveLength(0);
 });
 it('honours zero volume and rejects non-finite impact strengths',async()=>{
  const {preloadAudio,unlockAudio,playSound,playReaction,sound}=await import('./audio');await preloadAudio();unlockAudio();
  sound.collision=0;sound.reaction=0;playSound('cue',4);playReaction('😂');playSound('rail',NaN);
  expect(instances[0].sources).toHaveLength(0);
 });
 it('replaces a previous reaction without stopping table sounds',async()=>{
  const {preloadAudio,unlockAudio,playSound,playReaction}=await import('./audio');await preloadAudio();unlockAudio();
  playSound('collision',4);playReaction('😂');instances[0].currentTime=.6;playReaction('😮');
  const [impact,laugh,wow]=instances[0].sources;
  expect(impact.stop).not.toHaveBeenCalled();expect(laugh.stop).toHaveBeenCalledOnce();
  expect(wow.buffer?.sample).toBe('/audio/wow.wav');
 });
});

