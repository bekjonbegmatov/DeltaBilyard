import {useEffect,useId,useRef,useState} from 'react';
import {Check,Target} from 'lucide-react';
import {haptic} from './haptics';
import './pocket-picker.css';

// Same order as Physics.POCKETS / tableGeometry.pockets, viewed from the top.
const pocketNames=['Слева сверху','По центру сверху','Справа сверху','Слева снизу','По центру снизу','Справа снизу'];
export function pocketLabel(index:number){return pocketNames[index]?`Луза ${index+1} · ${pocketNames[index].toLocaleLowerCase('ru')}`:'Луза не выбрана'}
type Props={initial:number;onConfirm:(index:number)=>void;onCancel:()=>void};

export default function PocketPicker({initial,onConfirm,onCancel}:Props){
 const [selected,setSelected]=useState(initial>=0&&initial<6?initial:-1);
 const descriptionId=useId();
 const buttons=useRef<(HTMLButtonElement|null)[]>([]);
 useEffect(()=>setSelected(initial>=0&&initial<6?initial:-1),[initial]);
 function select(index:number){setSelected(index);haptic('tick')}
 function move(event:React.KeyboardEvent<HTMLButtonElement>,index:number){
  const next=event.key==='ArrowRight'?(index%3===2?index-2:index+1):event.key==='ArrowLeft'?(index%3===0?index+2:index-1):event.key==='ArrowUp'||event.key==='ArrowDown'?(index+3)%6:event.key==='Home'?0:event.key==='End'?5:-1;
  if(next<0)return;event.preventDefault();select(next);buttons.current[next]?.focus();
 }
 return <div className="pocket-picker">
  <p className="pocket-picker-instruction" id={descriptionId}>Нажмите на нужную лузу на схеме и подтвердите выбор. Стол показан сверху.</p>
  <div className="pocket-picker-map" role="radiogroup" aria-label="Луза для восьмёрки" aria-describedby={descriptionId}>
   <div className="pocket-picker-felt" aria-hidden="true"><span className="pocket-picker-eight">8</span></div>
   {pocketNames.map((name,index)=><button key={name} type="button" role="radio" aria-checked={selected===index} aria-label={`Луза ${index+1}: ${name}`} className={'pocket-picker-pocket pocket-picker-pocket-'+index+(selected===index?' is-selected':'')} ref={node=>{buttons.current[index]=node}} tabIndex={selected===index||(selected<0&&index===0)?0:-1} onClick={()=>select(index)} onKeyDown={event=>move(event,index)}><span>{index+1}</span>{selected===index&&<Check size={15} aria-hidden="true"/>}</button>)}
  </div>
  <div className="pocket-picker-selection" aria-live="polite"><Target size={17}/><span>{selected<0?'Выберите одну из шести луз':pocketLabel(selected)}</span></div>
  <div className="pocket-picker-actions"><button type="button" className="pocket-picker-cancel" onClick={onCancel}>Отмена</button><button type="button" className="pocket-picker-confirm" disabled={selected<0} onClick={()=>{haptic();onConfirm(selected)}}>{selected<0?'Подтвердить':`Выбрать лузу ${selected+1}`}<Check size={18}/></button></div>
 </div>
}
