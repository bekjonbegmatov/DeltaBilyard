import type {Room,RuleSet} from './types';
export const ruleLabels:Record<RuleSet,string>={eight:'Восьмёрка',nine:'Девятка',free:'Свободный пул'};
export const ruleDescriptions:Record<RuleSet,string>={eight:'Своя группа шаров, затем восьмёрка в выбранную лузу.',nine:'Сначала младший номер. Правильно забитая девятка приносит победу.',free:'Любой шар — очко. Побеждает первый, кто наберёт 8.'};
export const normalizedRuleSet=(value?:string):RuleSet=>value==='nine'||value==='free'?value:'eight';
export function legalTargetIds(room:Room,team:number):number[]{
 const remaining=room.balls.filter(b=>b.id>0&&!b.pocketed).map(b=>b.id).sort((a,b)=>a-b);
 if(room.breaking)return remaining.includes(1)?[1]:[];
 const rules=normalizedRuleSet(room.ruleSet);
 if(rules==='free')return remaining;
 if(rules==='nine')return remaining.length?[remaining[0]]:[];
 const group=room.groups[team]||0;
 if(!group)return remaining.filter(id=>id!==8);
 const ids=remaining.filter(id=>group===1?id>=1&&id<=7:id>=9&&id<=15);
 return ids.length?ids:remaining.filter(id=>id===8);
}
export function groupIds(group:number):number[]{return group===1?[1,2,3,4,5,6,7]:group===2?[9,10,11,12,13,14,15]:[]}
export function targetLabel(room:Room,team:number):string{
 const rules=normalizedRuleSet(room.ruleSet),ids=legalTargetIds(room,team);
 if(room.breaking)return 'Разбой · сначала №1';
 if(rules==='nine')return ids.length?'Первый контакт · №'+ids[0]:'Девятка забита';
 if(rules==='free')return room.mode==='practice'?'Любой шар · осталось '+ids.length:'Любой шар · '+(room.rackPoints?.[team]||0)+'/8 очков';
 if(ids.length===1&&ids[0]===8)return 'Восьмёрка · выберите лузу';
 return room.groups[team]===1?'Ваши: 1–7 · без полосы':room.groups[team]===2?'Ваши: 9–15 · с полосой':'Группа ещё не определена';
}
