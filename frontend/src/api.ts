let token=sessionStorage.getItem('delta-token')||'';
export function setToken(t:string){token=t;sessionStorage.setItem('delta-token',t)}
export async function api<T=any>(path:string,body?:unknown):Promise<T>{const r=await fetch('/api'+path,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})});const n=await r.json().catch(()=>({error:'Сервер временно недоступен'}));if(!r.ok)throw new Error(n.error||'Не удалось выполнить запрос');return n}
let removeTelegramListeners:undefined|(()=>void);
export function setupTelegram(){
 const t=window.Telegram?.WebApp;if(!t)return;
 t.ready();t.expand();t.setHeaderColor?.('#101916');t.setBackgroundColor?.('#101916');
 const update=()=>{
  document.documentElement.dataset.tgFullscreen=t.isFullscreen?'true':'false';
  document.documentElement.style.setProperty('--tg-device-top',(t.safeAreaInset?.top||0)+'px');
  for(const edge of ['top','bottom','left','right'] as const){
   const device=t.safeAreaInset?.[edge]||0;
   // Telegram controls sit inside the device-safe viewport. Keep both insets.
   // Older fullscreen clients can briefly report zero before their inset event.
   const content=t.contentSafeAreaInset?.[edge]||(edge==='top'&&t.isFullscreen?44:0);
   document.documentElement.style.setProperty('--tg-'+edge,(device+content)+'px');
  }
 };
 removeTelegramListeners?.();
 const events=['safeAreaChanged','contentSafeAreaChanged','viewportChanged','fullscreenChanged'];
 events.forEach(event=>t.onEvent?.(event,update));
 removeTelegramListeners=()=>events.forEach(event=>t.offEvent?.(event,update));
 if(t.isVersionAtLeast?.('8.0')){t.requestFullscreen?.();t.disableVerticalSwipes?.();}
 update();
}
