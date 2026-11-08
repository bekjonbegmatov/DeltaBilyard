import type {Shot} from './types';
export function replayAt(shot:Shot,elapsed:number){
 const pots=shot.events.filter(e=>e.type==='pocket'&&e.ball!==0);
 if(!pots.length||elapsed<shot.duration)return null;
 const total=Math.min(3,Math.max(2,pots.length)),time=elapsed-shot.duration,index=Math.floor(time/2.4);
 if(index>=total)return null;
 const event=pots[index%pots.length],start=Math.max(0,event.t-1.1),end=Math.min(shot.duration,event.t+.22);
 return {event,index,total,t:start+(end-start)*Math.min(1,(time-index*2.4)/2.15)};
}
