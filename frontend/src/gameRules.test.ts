import {describe,it,expect} from 'vitest';
import {legalTargetIds,normalizedRuleSet,targetLabel} from './gameRules';
import type {Room} from './types';
const makeRoom=(changes:Partial<Room>={}):Room=>({ruleSet:'eight',groups:[1,2],breaking:false,mode:'duel',balls:Array.from({length:16},(_,id)=>({id,x:0,y:0,pocketed:false})),...changes} as Room);
describe('visible targets match the selected rules',()=>{
 it('keeps legacy rooms on eight-ball and requires the whole group before eight',()=>{const room=makeRoom({ruleSet:undefined});expect(normalizedRuleSet(room.ruleSet)).toBe('eight');room.balls[1].pocketed=true;expect(legalTargetIds(room,0)).toEqual([2,3,4,5,6,7]);for(let id=2;id<=7;id++)room.balls[id].pocketed=true;expect(legalTargetIds(room,0)).toEqual([8]);expect(targetLabel(room,0)).toContain('выберите лузу')});
 it('does not invite an eight-ball call on an open table with only eight left',()=>{const room=makeRoom({groups:[0,0]});room.balls.forEach(b=>b.pocketed=b.id!==0&&b.id!==8);expect(legalTargetIds(room,0)).toEqual([])});
 it('selects the smallest live nine-ball number regardless of old group assignments',()=>{const room=makeRoom({ruleSet:'nine'});room.balls=room.balls.slice(0,10);room.balls[1].pocketed=true;room.balls[2].pocketed=true;expect(legalTargetIds(room,1)).toEqual([3]);expect(targetLabel(room,0)).toBe('Первый контакт · №3')});
 it('shows all remaining free-pool targets and the team score',()=>{const room=makeRoom({ruleSet:'free',rackPoints:[4,3]});room.balls[1].pocketed=true;expect(legalTargetIds(room,0)).toContain(8);expect(legalTargetIds(room,0)).not.toContain(1);expect(targetLabel(room,0)).toContain('4/8')});
 it('shows the head ball on break rather than prematurely assigning a group',()=>{expect(legalTargetIds(makeRoom({breaking:true,ruleSet:'nine'}),0)).toEqual([1]);expect(targetLabel(makeRoom({breaking:true}),0)).toBe('Разбой · сначала №1')});
});
