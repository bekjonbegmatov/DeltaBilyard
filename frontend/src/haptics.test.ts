import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';
const selectionChanged=vi.fn(),impactOccurred=vi.fn(),notificationOccurred=vi.fn(),vibrate=vi.fn(()=>true);
beforeEach(()=>{
 vi.resetModules();vi.clearAllMocks();vi.useFakeTimers();vi.setSystemTime(10000);
 vi.stubGlobal('window',{Telegram:{WebApp:{isVersionAtLeast:vi.fn(()=>true),HapticFeedback:{selectionChanged,impactOccurred,notificationOccurred}}}});
 vi.stubGlobal('navigator',{userAgent:'Android',vibrate});
});
afterEach(()=>{vi.unstubAllGlobals();vi.useRealTimers()});
describe('device haptics',()=>{
 it('uses Telegram selection feedback for ruler changes and impact feedback for taps',async()=>{
  const {haptic}=await import('./haptics');haptic('tick');haptic();haptic('shot');
  expect(selectionChanged).toHaveBeenCalledOnce();expect(impactOccurred.mock.calls).toEqual([['light'],['medium']]);
  expect(window.Telegram?.WebApp.isVersionAtLeast).toHaveBeenCalledWith('6.1');expect(vibrate).not.toHaveBeenCalled();
 });
 it('limits rapid collisions but still signals a pot separately',async()=>{
  const {haptic}=await import('./haptics');haptic('collision',.1);haptic('collision',3);haptic('rail',3);haptic('pot');
  expect(impactOccurred).toHaveBeenCalledOnce();expect(notificationOccurred).toHaveBeenCalledWith('success');
  vi.advanceTimersByTime(90);haptic('rail',3);expect(impactOccurred).toHaveBeenCalledTimes(2);
 });
 it('uses Android vibration when the Telegram version is unsupported',async()=>{
  vi.mocked(window.Telegram!.WebApp.isVersionAtLeast!).mockReturnValue(false);
  const {haptic}=await import('./haptics');haptic('shot');expect(impactOccurred).not.toHaveBeenCalled();expect(vibrate).toHaveBeenCalledWith(22);
 });
 it('falls back after a failed bridge without throwing into gameplay',async()=>{
  impactOccurred.mockImplementationOnce(()=>{throw new Error('bridge unavailable')});
  const {haptic}=await import('./haptics');expect(()=>haptic('shot')).not.toThrow();expect(vibrate).toHaveBeenCalledWith(22);
 });
 it('does not pretend to provide browser vibration on iOS',async()=>{
  vi.stubGlobal('window',{});vi.stubGlobal('navigator',{userAgent:'iPhone',vibrate});
  const {haptic}=await import('./haptics');haptic('shot');expect(vibrate).not.toHaveBeenCalled();
 });
});
