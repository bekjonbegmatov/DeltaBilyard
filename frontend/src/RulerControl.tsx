import {useRef,type PointerEvent,type KeyboardEvent,CSSProperties} from 'react';
import {haptic} from './haptics';
type Props={label:string;value:number;min:number;max:number;step:number;onChange:(v:number)=>void;relative?:boolean;disabled?:boolean;display:string};
export default function RulerControl({label,value,min,max,step,onChange,relative=false,disabled=false,display}:Props){
 const drag=useRef<{y:number;value:number}|null>(null),last=useRef(value);
 function update(v:number){const n=relative?((v+Math.PI)%(Math.PI*2)+Math.PI*2)%(Math.PI*2)-Math.PI:Math.max(min,Math.min(max,v));if(Math.abs(n-last.current)>=step*2){haptic('tick');last.current=n}onChange(n)}
 function down(e:PointerEvent<HTMLDivElement>){if(disabled)return;e.currentTarget.setPointerCapture(e.pointerId);drag.current={y:e.clientY,value};}
 function move(e:PointerEvent<HTMLDivElement>){if(!drag.current||disabled)return;const length=e.currentTarget.getBoundingClientRect().height;update(drag.current.value+(drag.current.y-e.clientY)*(relative?.0018:(max-min)/Math.max(130,length)))}
 function key(e:KeyboardEvent<HTMLDivElement>){if(disabled)return;let v=value;if(e.key==='ArrowUp'||e.key==='ArrowRight')v+=step;else if(e.key==='ArrowDown'||e.key==='ArrowLeft')v-=step;else if(e.key==='Home')v=min;else if(e.key==='End')v=max;else return;e.preventDefault();update(v)}
 const progress=relative?((value/(Math.PI*2))%1+1)%1:(value-min)/(max-min);
 return <div className={'touch-ruler '+(disabled?'disabled':'')}><span className="ruler-name">{label}</span><strong>{display}</strong><div className="ruler-track" role="slider" tabIndex={disabled?-1:0} aria-label={label} aria-valuemin={min} aria-valuemax={max} aria-valuenow={value} aria-valuetext={display} aria-disabled={disabled} aria-orientation="vertical" onKeyDown={key} onPointerDown={down} onPointerMove={move} onPointerUp={()=>{drag.current=null}} onPointerCancel={()=>{drag.current=null}} style={{'--ruler-shift':`${progress*160}px`,'--ruler-fill':`${progress*100}%`} as CSSProperties}><div className="ruler-ticks"/><div className="ruler-marker"/><span className="ruler-center"/></div><span className="ruler-hint">{relative?'ТОЧНО':'СИЛА'}</span></div>
}
