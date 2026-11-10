import {Check} from 'lucide-react';
import {botLevels,botLevelNames,botLevelDescriptions} from './botLevels';
import type {BotLevel} from './types';

export default function BotLevelPicker({value,onChange}:{value:BotLevel;onChange:(value:BotLevel)=>void}){
 return <div className="bot-level-picker" role="group" aria-label="Уровень соперника">{botLevels.map(level=><button type="button" key={level} className={value===level?'chosen':''} aria-pressed={value===level} onClick={()=>onChange(level)}><span><strong>{botLevelNames[level]}</strong><small>{botLevelDescriptions[level]}</small></span>{value===level&&<Check size={16}/>}</button>)}</div>;
}
