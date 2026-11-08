import * as THREE from 'three';

// Deterministic, local material maps. No network textures or per-frame generation.
export function surfaceTextures() {
 let seed=19;
 const random=()=>{seed=(1664525*seed+1013904223)>>>0;return seed/4294967296};
 function texture(kind:'felt'|'wood') {
  const canvas=document.createElement('canvas');canvas.width=kind==='wood'?1024:256;canvas.height=256;
  const ctx=canvas.getContext('2d')!,data=ctx.createImageData(canvas.width,canvas.height);
  for(let y=0;y<canvas.height;y++)for(let x=0;x<canvas.width;x++){
   const i=(y*canvas.width+x)*4;
   if(kind==='felt'){
    const weave=((x%3===0?1:0)+(y%3===0?1:0))*4;
    const v=235+random()*12-weave;data.data[i]=data.data[i+1]=data.data[i+2]=v;
   }else{
    const w=y+Math.sin(x*.009)*3+Math.sin(x*.003)*9;
    const grain=Math.sin(w*.65)*3+Math.sin(w*2.1)*2+random()*3;
    const band=7*Math.sin(w*.08);data.data[i]=65+grain+band;data.data[i+1]=43+grain*.65+band*.7;data.data[i+2]=28+grain*.4+band*.5;
   }
   data.data[i+3]=255;
  }
  ctx.putImageData(data,0,0);
  const t=new THREE.CanvasTexture(canvas);t.wrapS=t.wrapT=THREE.RepeatWrapping;t.colorSpace=THREE.SRGBColorSpace;t.anisotropy=4;
  return t;
 }
 return {felt:texture('felt'),wood:texture('wood')};
}
