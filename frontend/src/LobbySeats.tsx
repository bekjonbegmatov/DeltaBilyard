import {useState} from 'react';
import {Check,Clock,Plus,ArrowLeftRight,Bot,UserRound,X} from 'lucide-react';
import {botLevels,botLevelNames} from './botLevels';
import type {Room,User,BotLevel} from './types';
type Props={room:Room;user:User|null;connected:boolean;command:(action:string,data?:object)=>void};
export default function LobbySeats({room,user,connected,command}:Props){
 const [level,setLevel]=useState<BotLevel>('medium');const capacity=room.mode==='team'?4:room.mode==='practice'?1:2;const host=(room.hostId||room.seats.find(s=>s.user.id>0)?.user.id)===user?.id;
 const slots=Array.from({length:capacity},(_,slot)=>({slot,seat:room.seats.find((s,i)=>(s.slot??i)===slot)}));
 const mySlot=room.seats.find((s)=>s.user.id===user?.id)?.slot??room.seats.findIndex(s=>s.user.id===user?.id);
 const requests=Object.entries(room.swapRequests||{}).filter(([,slot])=>slot===mySlot);
 const label=(slot:number)=>room.mode==='team'?'Команда '+(slot%2===0?'А':'Б')+' · место '+(Math.floor(slot/2)+1):slot===0?'Место 1':'Место 2';
 return <div className="lobby-seating"><div className={'lobby-slots '+(room.mode==='team'?'teams':'')}>
 {slots.map(({slot,seat})=><div className={'lobby-slot '+(seat?.user.id===user?.id?'mine':'')+' team-'+slot%2} key={slot}>
 <span className="seat-label">{label(slot)}</span>{seat?<><div className="seat-person">{seat.user.id<0?<Bot size={20}/>:seat.user.avatar?<img src={seat.user.avatar} alt=""/>:<UserRound size={20}/>}<span title={seat.user.name}>{seat.user.name}<small>{seat.user.id<0?botLevelNames[seat.botLevel||room.botLevel||'medium']:seat.ready?'Готов':'Выбирает место'}</small></span>{seat.ready?<Check size={16}/>:<Clock size={15}/>}</div><div className="seat-actions">{seat.user.id===user?.id?<span>Вы здесь</span>:seat.user.id>0?<button disabled={!connected} onClick={()=>command('seat',{slot})}><ArrowLeftRight size={13}/> Поменяться</button>:host?<><button disabled={!connected} onClick={()=>command('seat',{slot})} title="Поменяться местами с ботом"><ArrowLeftRight size={13}/> Занять</button><button disabled={!connected} onClick={()=>command('removeBot',{slot})} aria-label="Убрать бота"><X size={13}/></button></>:null}</div></>:<><p className="empty-seat">Свободно</p><div className="seat-actions"><button disabled={!connected} onClick={()=>command('seat',{slot})}>Занять</button>{host&&<button disabled={!connected} onClick={()=>command('addBot',{slot,level})}><Plus size={13}/> Бот</button>}</div></>}</div>)}
 </div>{host&&slots.some(s=>!s.seat)&&<label className="lobby-bot-level">Уровень добавляемого бота <select aria-label="Уровень бота для свободного места" value={level} onChange={e=>setLevel(e.target.value as BotLevel)}>{botLevels.map(value=><option key={value} value={value}>{botLevelNames[value]}</option>)}</select></label>}
 {requests.map(([id])=><div className="swap-request" key={id}><span>{room.seats.find(s=>String(s.user.id)===id)?.user.name||'Игрок'} предлагает поменяться местами</span><button disabled={!connected} onClick={()=>command('acceptSwap',{requester:Number(id)})}>Принять</button><button disabled={!connected} onClick={()=>command('declineSwap',{requester:Number(id)})}>Нет</button></div>)}
 {user&&room.swapRequests?.[String(user.id)]!==undefined&&<p className="swap-pending">Запрос обмена отправлен. Ждём согласия игрока.</p>}
 {room.mode==='team'&&<p className="seat-note">Выберите сторону до готовности. Обмен занятыми местами требует согласия второго игрока.</p>}
 </div>
}
