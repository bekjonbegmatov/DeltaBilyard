# Assets

`frontend/public/models/club-table.glb` и `club-room.glb` созданы программно в Blender для этого проекта. Исходник: `assets/blender/delta-club.blend`, воспроизводящий скрипт: `assets/blender/build_club.py`. Сцена исходного Blender-проекта сохранена отдельно; игровая геометрия находится в новой сцене DeltaBilyard Studio. В клиенте неподвижные меши объединяются по материалам для сокращения draw calls.

`frontend/public/images/club-characters.png` создан встроенным imagegen. Промпт: «Two original adult billiards club players in equal side-by-side portrait panels: man aged 35, short dark hair, slight stubble, charcoal waistcoat and white shirt; woman aged 32, tied-back dark brown hair, emerald waistcoat and ivory shirt. Waist-up, natural proportions, holding cues, mature cinematic game rendering, warm side light, dark emerald club background. No text, logos or cartoon exaggeration.» Изображение используется как атлас двух портретов, выбираемых в настройках профиля.

Все элементы выбора декоративные: персонажи и кии не дают игровых преимуществ. Свободная камера поддерживает вращение пальцем и zoom двумя пальцами. Цвет стола и оформление зала задаются при создании комнаты и одинаковы для её участников.
