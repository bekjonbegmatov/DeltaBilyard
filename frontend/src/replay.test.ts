import {describe,it,expect} from 'vitest';
import {replayAt} from './replay';
import {aimGuide} from './aimGuide';
import type {Shot} from './types';
const shot={duration:9,frames:[],events:[{t:2,type:'pocket',ball:3,pocket:0,strength:1},{t:7,type:'pocket',ball:9,pocket:2,strength:1}]} as Shot;
describe('finite event-centred replay',()=>{
 it('replays actual early pot, not end of shot',()=>{expect(replayAt(shot,9)?.t).toBeCloseTo(.9);expect(replayAt(shot,11.5)?.event.ball).toBe(9)});
 it('ends after all slots and does not wrap',()=>{expect(replayAt(shot,14)).toBeNull();expect(replayAt(shot,8)).toBeNull()});
 it('gives a single pot two angles and no scratch-only replay',()=>{expect(replayAt({...shot,events:[shot.events[0]]},12)?.total).toBe(2);expect(replayAt({...shot,events:[{...shot.events[0],ball:0}]},10)).toBeNull()});
});
it('guides first object contact and continuation',()=>{const g=aimGuide([{id:0,x:.5,y:.6,pocketed:false},{id:1,x:1,y:.6,pocketed:false},{id:2,x:1.5,y:.6,pocketed:false}],0);expect(g.ball).toBe(1);expect(g.ghost?.[0]).toBeCloseTo(1-.05715);expect(g.objectPath?.[1][0]).toBeGreaterThan(1)});
