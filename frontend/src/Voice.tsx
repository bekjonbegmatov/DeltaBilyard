import {useEffect,useRef,useState} from 'react';
import {Room,RoomEvent,Track} from 'livekit-client';
import {Mic,MicOff,Volume2,RefreshCw} from 'lucide-react';
import {api} from './api';

type VoiceState='connecting'|'connected'|'reconnecting'|'error';

/** Joining a table subscribes to its audio. Only an explicit click publishes a microphone. */
export default function Voice({code,onError}:{code:string;onError:(message:string)=>void}){
 const room=useRef<Room|null>(null),audio=useRef<HTMLDivElement>(null),report=useRef(onError);
 const [status,setStatus]=useState<VoiceState>('connecting');
 const [mic,setMic]=useState(false),[micBusy,setMicBusy]=useState(false),[audioBlocked,setAudioBlocked]=useState(false),[retry,setRetry]=useState(0);
 useEffect(()=>{report.current=onError},[onError]);
 useEffect(()=>{
  let disposed=false,attempt=0,generation=0,timer:ReturnType<typeof setTimeout>|undefined;
  const elements=new Set<HTMLMediaElement>();
  const removeElements=()=>{elements.forEach(element=>{element.pause();element.srcObject=null;element.remove()});elements.clear()};
  const unlock=()=>{
   const current=room.current;
   if(!current||current.canPlaybackAudio)return;
   void current.startAudio().then(()=>{if(!disposed&&room.current===current)setAudioBlocked(!current.canPlaybackAudio)}).catch(()=>{if(!disposed)setAudioBlocked(true)});
  };
  const reconnect=()=>{
   if(disposed)return;
   clearTimeout(timer);
   setStatus('reconnecting');
   timer=setTimeout(()=>void connect(),Math.min(30000,1500*2**Math.min(attempt++,4)));
  };
  const connect=async()=>{
   if(disposed)return;
   const turn=++generation;const previous=room.current;room.current=null;
   if(previous){previous.removeAllListeners();await previous.disconnect();}
   if(disposed||turn!==generation)return;
   removeElements();
   setMic(false);setMicBusy(false);setAudioBlocked(false);
   setStatus(attempt?'reconnecting':'connecting');
   const current=new Room({adaptiveStream:true,dynacast:true});
   room.current=current;
   const live=()=>!disposed&&room.current===current;
   const refreshMic=()=>{if(live())setMic(current.localParticipant.isMicrophoneEnabled)};
   current.on(RoomEvent.TrackSubscribed,track=>{
    if(!live()||track.kind!==Track.Kind.Audio)return;
    const element=track.attach();elements.add(element);audio.current?.appendChild(element);
    setAudioBlocked(!current.canPlaybackAudio);
   }).on(RoomEvent.TrackUnsubscribed,track=>track.detach().forEach(element=>{elements.delete(element);element.remove()}))
    .on(RoomEvent.AudioPlaybackStatusChanged,()=>{if(live())setAudioBlocked(!current.canPlaybackAudio)})
    .on(RoomEvent.LocalTrackPublished,refreshMic).on(RoomEvent.LocalTrackUnpublished,refreshMic)
    .on(RoomEvent.TrackMuted,refreshMic).on(RoomEvent.TrackUnmuted,refreshMic)
    .on(RoomEvent.Reconnecting,()=>{if(live())setStatus('reconnecting')})
    .on(RoomEvent.SignalReconnecting,()=>{if(live())setStatus('reconnecting')})
    .on(RoomEvent.Reconnected,()=>{if(live()){attempt=0;setStatus('connected');refreshMic();unlock()}})
    .on(RoomEvent.Disconnected,()=>{if(live()){setMic(false);reconnect()}});
   try{
    const credentials=await api<{url:string;token:string}>('/rooms/'+code+'/voice',{});
    if(!live())return;
    await current.connect(credentials.url,credentials.token);
    if(!live()){await current.disconnect();return;}
    attempt=0;setStatus('connected');refreshMic();
    // Listening is attempted automatically; browsers may defer it until a user gesture.
    try{await current.startAudio()}catch{/* The explicit sound button remains available. */}
    if(live())setAudioBlocked(!current.canPlaybackAudio);
   }catch{
    if(!live())return;
    current.removeAllListeners();await current.disconnect();
    if(!live())return;
    room.current=null;setStatus('error');setMic(false);
    clearTimeout(timer);timer=setTimeout(()=>void connect(),Math.min(30000,2000*2**Math.min(attempt++,4)));
   }
  };
  const online=()=>{clearTimeout(timer);void connect()};
  window.addEventListener('pointerdown',unlock,{capture:true});
  window.addEventListener('keydown',unlock,{capture:true});
  window.addEventListener('online',online);
  void connect();
  return()=>{
   disposed=true;clearTimeout(timer);
   window.removeEventListener('pointerdown',unlock,{capture:true});
   window.removeEventListener('keydown',unlock,{capture:true});
   window.removeEventListener('online',online);
   const current=room.current;room.current=null;
   current?.removeAllListeners();void current?.disconnect();removeElements();
  };
 },[code,retry]);
 async function toggleMic(){
  const current=room.current;if(!current||status!=='connected'||micBusy)return;
  setMicBusy(true);
  try{
   // Start capture directly in the click handler, never on mount or reconnect.
   const change=current.localParticipant.setMicrophoneEnabled(!current.localParticipant.isMicrophoneEnabled,{echoCancellation:true,noiseSuppression:true,autoGainControl:true});
   void current.startAudio().catch(()=>setAudioBlocked(true));
   await change;
   if(room.current===current)setMic(current.localParticipant.isMicrophoneEnabled);
  }catch{
   if(room.current===current)report.current('Микрофон недоступен. Разрешите доступ в настройках Telegram — слушать остальных можно без микрофона.');
  }finally{if(room.current===current)setMicBusy(false)}
 }
 async function startListening(){
  const current=room.current;if(!current)return;
  try{await current.startAudio();if(room.current===current)setAudioBlocked(!current.canPlaybackAudio)}
  catch{report.current('Телефон приостановил звук. Коснитесь кнопки звука ещё раз или вернитесь в Telegram.');}
 }
 const label=status==='connected'?(mic?'Выключить микрофон':'Включить микрофон'):status==='error'?'Голос временно недоступен':'Подключение голоса…';
 return <div className="voice"><div ref={audio} hidden/>
  <button className={'icon-button '+(mic?'active':'')} disabled={status!=='connected'||micBusy} onClick={()=>void toggleMic()} aria-label={label} aria-pressed={mic} title={label}>{mic?<Mic size={18}/>:<MicOff size={18}/>}</button>
  {status==='connected'&&audioBlocked&&<button className="icon-button voice-retry" onClick={()=>void startListening()} aria-label="Включить звук участников" title="Включить звук участников"><Volume2 size={18}/></button>}
  {status==='error'&&<button className="icon-button voice-retry" onClick={()=>setRetry(value=>value+1)} aria-label="Повторить подключение голоса" title="Повторить подключение голоса"><RefreshCw size={18}/></button>}
 </div>
}
