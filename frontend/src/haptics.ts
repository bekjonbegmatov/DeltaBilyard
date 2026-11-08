export type HapticKind='tap'|'tick'|'shot'|'collision'|'rail'|'pot'|'success';
let lastTick=-Infinity,lastImpact=-Infinity,lastPot=-Infinity;

/** Telegram 6.1+ on supported devices; short Android browser vibration as a fallback. */
export function haptic(kind:HapticKind='tap',strength=1){
 if(typeof window==='undefined'||typeof navigator==='undefined')return;
 const now=Date.now();
 if(kind==='tick'){if(now-lastTick<45)return;lastTick=now}
 if(kind==='collision'||kind==='rail'){
  if(!Number.isFinite(strength)||strength<.35||now-lastImpact<85)return;
  lastImpact=now;
 }
 if(kind==='pot'||kind==='success'){if(now-lastPot<160)return;lastPot=now}
 try{
  const tg=window.Telegram?.WebApp;
  if(tg?.isVersionAtLeast?.('6.1')&&tg.HapticFeedback){
   const feedback=tg.HapticFeedback;
   if(kind==='tick')feedback.selectionChanged();
   else if(kind==='success'||kind==='pot')feedback.notificationOccurred('success');
   else feedback.impactOccurred(kind==='shot'?'medium':kind==='collision'?'rigid':'light');
   return;
  }
 }catch{/* An unavailable Telegram bridge can still use Android's vibration API. */}
 try{
  if(/Android/i.test(navigator.userAgent)&&typeof navigator.vibrate==='function'){
   navigator.vibrate(kind==='tick'?6:kind==='shot'?22:kind==='pot'||kind==='success'?[12,30,12]:9);
  }
 }catch{/* Device/system settings may disable haptics; gameplay is unaffected. */}
}
