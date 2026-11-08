import {useState,useRef,useEffect} from 'react';
import {Room,RoomEvent,Track} from 'livekit-client';
import {Mic,MicOff,Headphones} from 'lucide-react';
import {api} from './api';
export default function Voice({code,onError}:{code:string;onError:(e:string)=>void}){const room=useRef<Room|null>(null),audio=useRef<HTMLDivElement>(null);const [on,setOn]=useState(false),[mic,setMic]=useState(false),[busy,setBusy]=useState(false),[people,setPeople]=useState<{id:string;name:string;muted:boolean}[]>([]);
 useEffect(()=>()=>{void room.current?.disconnect()},[code]);
 async function connect(){setBusy(true);try{const v=await api('/rooms/'+code+'/voice',{});const r=new Room({adaptiveStream:true,dynacast:true});room.current=r;
 const refresh=()=>setPeople([...r.remoteParticipants.values()].map(p=>({id:p.identity,name:p.name||p.identity,muted:false})));
 r.on(RoomEvent.TrackSubscribed,(track)=>{if(track.kind===Track.Kind.Audio){const el=track.attach();audio.current?.appendChild(el)}}).on(RoomEvent.TrackUnsubscribed,track=>track.detach().forEach(el=>el.remove())).on(RoomEvent.ParticipantConnected,refresh).on(RoomEvent.ParticipantDisconnected,refresh).on(RoomEvent.Disconnected,()=>{setOn(false);setMic(false);setPeople([])});
 await r.connect(v.url,v.token);await r.startAudio();setOn(true);refresh();}catch(e){onError((e as Error).message);await room.current?.disconnect();room.current=null}finally{setBusy(false)}}
 async function toggleMic(){try{await room.current?.localParticipant.setMicrophoneEnabled(!mic,{echoCancellation:true,noiseSuppression:true,autoGainControl:true});setMic(!mic)}catch{onError('Нет доступа к микрофону. Разрешите его в настройках Telegram.')}}
 return <div className="voice"><div ref={audio} hidden/>{!on?<button className="icon-button" disabled={busy} onClick={connect} title="Подключиться к голосу"><Headphones size={18}/><span>{busy?'Подключение…':'Голос'}</span></button>:<><button className={'icon-button '+(mic?'active':'')} onClick={toggleMic}>{mic?<Mic size={18}/>:<MicOff size={18}/>}<span>{mic?'Микрофон включён':'Микрофон выкл.'}</span></button><button className="text-button" onClick={()=>void room.current?.disconnect()}>Выйти из голоса</button>{people.map(p=><button key={p.id} className="text-button" onClick={()=>{const next=!p.muted;room.current?.remoteParticipants.get(p.id)?.setVolume(next?0:1);setPeople(v=>v.map(x=>x.id===p.id?{...x,muted:next}:x))}}>{p.muted?'🔇':'🔊'} {p.name}</button>)}</>}</div>
}
