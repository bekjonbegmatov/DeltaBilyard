import assert from 'node:assert/strict';
import {request,player,connect,until} from './test-client.mjs';

const seed=Date.now()%100000000,host=await player(seed),partner=await player(seed+1);
const room=await request('/rooms',{mode:'team',seconds:60,target:1,hints:true},host.token);
const a=await connect(host,room.code);let b;
try{
 await a.command('seat',{slot:2});
 await request('/rooms/'+room.code+'/join',{},partner.token);
 b=await connect(partner,room.code);
 await until(()=>a.room.seats.length===2);
 assert.equal(b.room.seats.find(s=>s.user.id===partner.id).slot,0);
 await a.command('seat',{slot:0});
 await until(()=>b.room.swapRequests[String(host.id)]===0);
 assert.equal(a.room.seats.find(s=>s.user.id===host.id).slot,2,'request must not move anyone before consent');
 await b.command('acceptSwap',{requester:host.id});
 await until(()=>a.room.seats.find(s=>s.user.id===host.id).slot===0);
 await a.command('addBot',{slot:1,level:'beginner'});
 await a.command('addBot',{slot:3,level:'pro'});
 assert.deepEqual(a.room.seats.map(s=>s.slot),[0,1,2,3]);
 assert.equal(new Set(a.room.seats.map(s=>s.user.id)).size,4);
 await a.command('ready');await until(()=>b.room.seq===a.room.seq);await b.command('ready');
 await until(()=>a.room.phase==='AIM');
 await a.command('shoot',{angle:Math.PI,power:.2,spinX:0,spinY:0});
 await until(()=>a.room.phase==='SHOT'&&a.room.lastShooterId<0,45000);
 assert.equal(a.room.lastShooterId,a.room.seats[1].user.id);
 assert.ok(a.room.shot.events.some(e=>e.type==='collision'));
 console.log('PASS team seats, consent swap, mixed bot levels and server bot turn');
}finally{
 if(a.room.phase!=='FINISHED')await a.command('leave');
 a.socket.close();b?.socket.close();
}
