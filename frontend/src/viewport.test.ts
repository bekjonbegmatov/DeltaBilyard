import {describe,it,expect} from 'vitest';
import {viewportSize} from './viewport';
describe('live viewport bounds',()=>{
 it('uses landscape window bounds if visual viewport is still portrait',()=>expect(viewportSize({width:844,height:390},{width:390,height:844,scale:1})).toEqual({width:844,height:390}));
 it('rejects a stale portrait visual height at the new width',()=>expect(viewportSize({width:844,height:390},{width:844,height:844,scale:1})).toEqual({width:844,height:390}));
 it('uses the smaller visible area when the keyboard covers part of the viewport',()=>expect(viewportSize({width:390,height:844},{width:390,height:510,scale:1})).toEqual({width:390,height:510}));
 it('does not resize the game to pinch zoom magnification',()=>expect(viewportSize({width:844,height:390},{width:422,height:195,scale:2})).toEqual({width:844,height:390}));
});
