import type {Ball} from './types';
import {W,H,R,pockets} from './tableGeometry';
type V=[number,number];
export type Guide={path:V[];ghost?:V;objectPath?:V[];cuePath?:V[];ball?:number};
function wall(p:V,d:V){const tx=Math.abs(d[0])<1e-8?Infinity:((d[0]>0?W-R:R)-p[0])/d[0],ty=Math.abs(d[1])<1e-8?Infinity:((d[1]>0?H-R:R)-p[1])/d[1];const t=Math.max(0,Math.min(tx,ty)),ex=p[0]+d[0]*t,ey=p[1]+d[1]*t;const pocket=pockets.some(q=>Math.hypot(ex-q[0],ey-q[1])<.10);return {t:t+(pocket?.08:0),normal:tx<ty?0:1,pocket}}
export function aimGuide(balls:Ball[],angle:number):Guide{
 const cue=balls.find(b=>b.id===0);if(!cue||cue.pocketed)return {path:[]};
 const p:V=[cue.x,cue.y],d:V=[Math.cos(angle),Math.sin(angle)],bank=wall(p,d);let distance=bank.t,target:Ball|undefined;
 for(const b of balls){if(!b.id||b.pocketed)continue;const dx=b.x-p[0],dy=b.y-p[1],proj=dx*d[0]+dy*d[1],perp=dx*dx+dy*dy-proj*proj;if(proj<=0||perp>4*R*R)continue;const hit=proj-Math.sqrt(Math.max(0,4*R*R-perp));if(hit>=0&&hit<distance){distance=hit;target=b}}
 const end:V=[p[0]+d[0]*distance,p[1]+d[1]*distance];
 if(!target&&bank.pocket)return {path:[p,end]};
 if(!target){const reflect:V=[...d];reflect[bank.normal]*=-1;return {path:[p,end,[end[0]+reflect[0]*.45,end[1]+reflect[1]*.45]]}}
 const n:V=[(target.x-end[0])/(2*R),(target.y-end[1])/(2*R)],dot=d[0]*n[0]+d[1]*n[1],res:V=[d[0]-n[0]*dot,d[1]-n[1]*dot];
 const length=Math.min(.65,wall([target.x,target.y],n).t);
 return {path:[p,end],ghost:end,ball:target.id,objectPath:[[target.x,target.y],[target.x+n[0]*length,target.y+n[1]*length]],cuePath:[end,[end[0]+res[0]*.45,end[1]+res[1]*.45]]};
}
