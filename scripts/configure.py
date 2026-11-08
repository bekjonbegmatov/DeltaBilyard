#!/usr/bin/env python3
"""Validate environment without printing credentials; render LiveKit config."""
import os,pathlib,sys,json
root=pathlib.Path(__file__).resolve().parent.parent
path=root/'.env'
if not path.exists(): sys.exit('Create .env from .env.example before starting.')
env={}
for line in path.read_text().splitlines():
    if line.strip() and not line.lstrip().startswith('#'):
        key,_,value=line.partition('=');env[key.strip()]=value.strip()
for key in ['POSTGRES_PASSWORD','REDIS_PASSWORD','SESSION_SECRET','ADMIN_KEY','LIVEKIT_API_SECRET']:
    value=env.get(key,'')
    if len(value)<32 or value.startswith('replace-'):sys.exit(key+' must contain at least 32 random characters')
if env.get('DEV_AUTH','false')!='false' and env.get('BASE_URL','').startswith('https://'):sys.exit('DEV_AUTH must be false on a public server')
if not env.get('BOT_TOKEN') and env.get('DEV_AUTH')!='true':sys.exit('BOT_TOKEN is required')
config='''port: 7880
rtc:
  tcp_port: 7881
  udp_port: 7882
  use_external_ip: true
  enable_loopback_candidate: false
keys:
  %s: %s
turn:
  enabled: true
  udp_port: 3478
room:
  auto_create: true
  empty_timeout: 300
  max_participants: 4
logging:
  level: warn
'''%(json.dumps(env.get('LIVEKIT_API_KEY','deltabilyard')),json.dumps(env['LIVEKIT_API_SECRET']))
(root/'infra/tls').mkdir(exist_ok=True)
if env.get('TURN_TLS')=='true':
    for filename in ['turn.crt','turn.key']:
        if not (root/'infra/tls'/filename).exists():sys.exit('Missing TURN certificate: '+filename)
    config=config.replace('  udp_port: 3478', '  udp_port: 3478\n  tls_port: 5349\n  domain: '+json.dumps(env['TURN_DOMAIN'])+'\n  cert_file: /etc/livekit-tls/turn.crt\n  key_file: /etc/livekit-tls/turn.key')
output=root/'infra/livekit.generated.yaml';output.write_text(config);output.chmod(0o600);path.chmod(0o600)
print('Configuration valid. LiveKit configuration rendered.')
