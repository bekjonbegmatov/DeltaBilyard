import {performance} from 'node:perf_hooks';
import {request,player,connect,until} from './test-client.mjs';
const count=Number(process.env.PAIRS||50),seed=Date.now()%100000000,clients=[],times=[];
for(let i=0;i<count;i++){const a=await player(seed+i*2),b=await player(seed+i*2+1);const r=await request('/rooms',{mode:'duel',seconds:60,target:1,hints:true},a.token);await request('/rooms/'+r.code+'/join',{},b.token);const ca=await connect(a,r.code),cb=await connect(b,r.code);await ca.command('ready');await until(()=>cb.room.seq===ca.room.seq);await cb.command('ready');await until(()=>ca.room.phase==='AIM');clients.push([ca,cb]);}
await Promise.all(clients.map(async([a,b])=>{const start=performance.now();await a.command('shoot',{angle:0,power:.8,spinX:0,spinY:0});await until(()=>b.room.seq===a.room.seq);times.push(performance.now()-start)}));
times.sort((a,b)=>a-b);console.log(JSON.stringify({players:count*2,matches:count,commands:times.length,p50_ms:times[Math.floor(times.length*.5)],p95_ms:times[Math.floor(times.length*.95)],max_ms:times.at(-1),measurement:'client end-to-end including simulation, database persistence and both websocket deliveries'},null,2));
for(const [a,b] of clients){await a.command('leave');a.socket.close();b.socket.close();}
