let lastTick=0;
export function haptic(kind:'tick'|'shot'|'pot'|'success'='tick') {
 const now=Date.now();if(kind==='tick'&&now-lastTick<45)return;if(kind==='tick')lastTick=now;
 try{
  const feedback=window.Telegram?.WebApp.HapticFeedback;
  if(feedback){if(kind==='tick')feedback.selectionChanged();else if(kind==='success'||kind==='pot')feedback.notificationOccurred('success');else feedback.impactOccurred('medium');return}
  navigator.vibrate?.(kind==='tick'?6:kind==='shot'?22:[12,30,12]);
 }catch{/* Haptics are optional on unsupported devices. */}
}
