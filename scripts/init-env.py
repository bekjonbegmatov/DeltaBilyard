#!/usr/bin/env python3
import getpass,pathlib,secrets
root=pathlib.Path(__file__).resolve().parent.parent
p=root/'.env'
if p.exists():raise SystemExit('.env already exists; refusing to replace secrets')
token=getpass.getpass('Telegram bot token (hidden): ')
values={'POSTGRES_USER':'bilyard','POSTGRES_PASSWORD':secrets.token_hex(32),'REDIS_PASSWORD':secrets.token_hex(32),'SESSION_SECRET':secrets.token_hex(32),'ADMIN_KEY':secrets.token_hex(32),'BOT_TOKEN':token,'BOT_NAME':'mikelanjelo_robot','BASE_URL':'https://bilyard.deltavpn.me','DEV_AUTH':'false','LIVEKIT_URL':'wss://bilyard.deltavpn.me/livekit','LIVEKIT_API_KEY':'deltabilyard','LIVEKIT_API_SECRET':secrets.token_hex(32),'PUBLIC_IP':'144.31.28.25','TURN_DOMAIN':'bilyard.deltavpn.me','TURN_TLS':'true'}
p.touch(mode=0o600);p.write_text(''.join(k+'='+v+'\n' for k,v in values.items()));print('Environment saved with mode 0600; values not logged.')
