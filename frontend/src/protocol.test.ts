import {describe,it,expect} from 'vitest';
import {rack} from './Table';
describe('table coordinates',()=>{it('contains one cue, eight centered and all fifteen object balls',()=>{const b=rack();expect(b.map(x=>x.id)).toEqual(Array.from({length:16},(_,i)=>i));expect(b[8].y).toBeCloseTo(1.27/2);expect(b[0].x).toBeLessThan(b[1].x);expect(b.every(x=>x.x>0&&x.x<2.54&&x.y>0&&x.y<1.27)).toBe(true)})});
