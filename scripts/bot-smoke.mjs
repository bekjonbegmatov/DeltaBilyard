import assert from 'node:assert/strict';
import {request,player,connect,until} from './test-client.mjs';
const seed=Date.now()%100000000;
await Promise.all(['beginner','medium','pro'].map(async(botLevel,i)=>{
 const human=await player(seed+i),r=await request('/rooms',{mode:'bot',botLevel,ballStyle:i===1?'black':'ivory',seconds:60,target:1,hints:true},human.token);
 const client=await connect(human,r.code);
 try{
  assert.equal(r.seats.length,2);assert.equal(r.seats[1].user.id,-1);assert.equal(r.botLevel,botLevel);
  await client.command('ready');await client.command('shoot',{angle:Math.PI,power:.12,spinX:0,spinY:0});
  const humanShot=client.room.shotAt;
  await until(()=>client.room.shotAt>humanShot&&client.room.phase==='SHOT',45000);
  assert.equal(client.room.disconnected['-1'],undefined);
  assert.ok(client.room.shot.events.some(e=>e.type==='collision'));
  assert.ok(client.room.potted);
  console.log('PASS bot',botLevel,'style',client.room.ballStyle,'seq',client.room.seq);
 }finally{if(client.room.phase!=='FINISHED')await client.command('leave');client.socket.close()}
}));
