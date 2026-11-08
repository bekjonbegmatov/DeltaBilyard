#!/usr/bin/env python3
import pathlib,json,urllib.request
root=pathlib.Path(__file__).resolve().parent.parent
env=dict(line.split('=',1) for line in (root/'.env').read_text().splitlines() if line and not line.startswith('#'))
token=env['BOT_TOKEN'];url=env['BASE_URL']
for method,payload in [('setChatMenuButton',{'menu_button':{'type':'web_app','text':'Играть','web_app':{'url':url}}}),('setMyCommands',{'commands':[{'command':'start','description':'Открыть бильярдный клуб'}]}),('setMyDescription',{'description':'DeltaBilyard — бильярдный клуб с друзьями. 3D-пул, комнаты 1×1 и 2×2, голосовой чат и тренировка.'})]:
 try:
  request=urllib.request.Request('https://api.telegram.org/bot'+token+'/'+method,data=json.dumps(payload).encode(),headers={'Content-Type':'application/json'})
  result=json.load(urllib.request.urlopen(request,timeout=15))
  if not result.get('ok'):raise RuntimeError()
  print(method+': OK')
 except Exception:raise SystemExit(method+': failed (credentials are not logged)')
