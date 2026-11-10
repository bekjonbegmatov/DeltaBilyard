import assert from 'node:assert/strict';
import {request,player,connect,until} from './test-client.mjs';

// Uses only the isolated DEV_AUTH backend allowed by test-client.mjs.
const seed=Date.now()%100000000,botLevel=process.env.BOT_LEVEL||'pro';
for(const [index,ruleSet] of ['eight','nine','free'].entries()){
 const host=await player(seed+index);
 const initial=await request('/rooms',{mode:'bot',ruleSet,botLevel,seconds:60,target:1,hints:true},host.token);
 const client=await connect(host,initial.code);
 try{
  assert.equal(client.room.ruleSet,ruleSet);
  assert.equal(client.room.balls.length,ruleSet==='nine'?10:16);
  assert.deepEqual(client.room.rackPoints,[0,0]);
  await client.command('ready');
  assert.equal(client.room.phase,'AIM');
  // Intentional miss gives the bot ball in hand and tests its rule-specific plan.
  await client.command('shoot',{angle:Math.PI,power:.15,spinX:0,spinY:0});
  await until(()=>client.room.phase==='SHOT'&&client.room.lastShooterId<0,45000);
  assert.ok(client.room.shot.events.some(event=>event.type==='collision'),'bot must hit an object ball');
  if(ruleSet==='nine')assert.equal(client.room.shot.firstContact,1,'nine-ball bot must hit the lowest ball');
  const restored=await request('/rooms/'+initial.code,undefined,host.token);
  assert.equal(restored.ruleSet,ruleSet);
  console.log('PASS '+ruleSet+' / '+botLevel+': room, rack, bot, snapshot');
 }finally{
  if(client.room.phase!=='FINISHED')await client.command('leave');
  client.socket.close();
 }
}
