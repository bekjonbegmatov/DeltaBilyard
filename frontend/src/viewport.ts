/** Telegram's viewportHeight can lag one orientation; size the game from the live WebView. */
export function viewportSize(inner:{width:number;height:number},visual?:{width:number;height:number;scale:number}){
 const useVisual=visual&&visual.width>0&&visual.height>0&&Math.abs(visual.scale-1)<.02&&Math.abs(visual.width-inner.width)<Math.max(3,inner.width*.05);
 return {width:Math.max(1,Math.round(useVisual?visual.width:inner.width)),height:Math.max(1,Math.round(useVisual?Math.min(visual.height,inner.height):inner.height))};
}
export function installViewportSync(){
 let frame=0;let timers:ReturnType<typeof setTimeout>[]=[];let previous='';
 const measure=()=>{frame=0;const vv=window.visualViewport;const size=viewportSize({width:window.innerWidth,height:window.innerHeight},vv?{width:vv.width,height:vv.height,scale:vv.scale}:undefined);const key=size.width+'x'+size.height;if(previous===key)return;previous=key;const root=document.documentElement;root.style.setProperty('--app-viewport-width',size.width+'px');root.style.setProperty('--app-viewport-height',size.height+'px');root.dataset.appOrientation=window.innerWidth>window.innerHeight?'landscape':'portrait';window.dispatchEvent(new CustomEvent('app:viewport',{detail:size}));};
 const queue=()=>{if(frame)cancelAnimationFrame(frame);frame=requestAnimationFrame(measure)};
 const settle=()=>{queue();timers.forEach(clearTimeout);timers=[60,180,450,900].map(delay=>setTimeout(queue,delay));};
 const visible=()=>{if(!document.hidden)settle()};
 window.addEventListener('resize',queue);window.addEventListener('orientationchange',settle);window.addEventListener('pageshow',settle);window.visualViewport?.addEventListener('resize',queue);window.visualViewport?.addEventListener('scroll',queue);document.addEventListener('visibilitychange',visible);document.addEventListener('fullscreenchange',settle);window.screen.orientation?.addEventListener('change',settle);
 measure();
 return {refresh:settle,dispose(){if(frame)cancelAnimationFrame(frame);timers.forEach(clearTimeout);window.removeEventListener('resize',queue);window.removeEventListener('orientationchange',settle);window.removeEventListener('pageshow',settle);window.visualViewport?.removeEventListener('resize',queue);window.visualViewport?.removeEventListener('scroll',queue);document.removeEventListener('visibilitychange',visible);document.removeEventListener('fullscreenchange',settle);window.screen.orientation?.removeEventListener('change',settle)}};
}
