export const W=2.54,H=1.27,R=.028575;
export const pockets=[[-.04,-.04],[W/2,-.045],[W+.04,-.04],[-.04,H+.04],[W/2,H+.045],[W+.04,H+.04]];
export const cushions:number[][]=[];
for(const y of [0,H]){const sign=y===0?-1:1;cushions.push([.078,y,W/2-.068,y],[W/2+.068,y,W-.078,y],[W/2-.068,y,W/2-.053,y+sign*.1],[W/2+.068,y,W/2+.053,y+sign*.1])}
for(const x of [0,W])cushions.push([x,.078,x,H-.078]);
for(const x of [0,W])for(const y of [0,H]){const sx=x===0?-1:1,sy=y===0?-1:1;cushions.push([x-sx*.078,y,x-sx*.005,y+sy*.077],[x,y-sy*.078,x+sx*.077,y-sy*.005])}
