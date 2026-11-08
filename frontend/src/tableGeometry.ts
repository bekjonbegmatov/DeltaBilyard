export const W=2.54,H=1.27,R=.028575;
export const pockets=[[-.026,-.026],[W/2,-.032],[W+.026,-.026],[-.026,H+.026],[W/2,H+.032],[W+.026,H+.026]];
export const cushions:number[][]=[];
for(const y of [0,H]){const sign=y===0?-1:1;cushions.push([.074,y,W/2-.061,y],[W/2+.061,y,W-.074,y],[W/2-.061,y,W/2-.047,y+sign*.075],[W/2+.061,y,W/2+.047,y+sign*.075])}
for(const x of [0,W])cushions.push([x,.074,x,H-.074]);
for(const x of [0,W])for(const y of [0,H]){const sx=x===0?-1:1,sy=y===0?-1:1;cushions.push([x-sx*.074,y,x-sx*.005,y+sy*.060],[x,y-sy*.074,x+sx*.060,y-sy*.005])}
