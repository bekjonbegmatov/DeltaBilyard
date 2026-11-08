#!/usr/bin/env python3
import os,pathlib
root=pathlib.Path(__file__).resolve().parent.parent
env=dict(os.environ)
for line in (root/'.env').read_text().splitlines():
 if line and not line.startswith('#'):
  k,_,v=line.partition('=');env[k]=v
env['DATABASE_URL']='jdbc:postgresql://localhost:55432/bilyard'
env['SPRING_DATA_REDIS_PORT']='56379'
os.execvpe('java',['java','-jar',str(root/'backend/target/server-1.0.0.jar')],env)
