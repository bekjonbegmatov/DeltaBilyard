# DeltaBilyard

Telegram Mini App: 3D-пул «восьмёрка», 1×1/2×2, тренировка, друзья, приглашения, реакции и голос LiveKit.

## Стек

Java 21 / Spring Boot, PostgreSQL 17, Redis 7.4, React 19 / TypeScript / Three.js, LiveKit. Один авторитетный игровой процесс; горизонтальное масштабирование игрового процесса пока не поддерживается. Физика фиксированного шага 240 Гц, клиент получает запись удара 30 Гц и интерполирует её. Результат и запись сохраняются до подтверждения команды.

## Сервер

Нужны Docker Engine с Compose, Git, Make, Python 3 и curl. Рекомендуемый старт: 4 CPU / 8 GB RAM. Пользователь запуска должен иметь доступ к Docker (либо запускать `sudo make build`).

```sh
git clone https://github.com/bekjonbegmatov/DeltaBilyard.git
cd DeltaBilyard
cp .env.example .env
# Заполнить .env. Каждый пароль/секрет: openssl rand -hex 32
make build
```

`.env` не коммитится. На публичном сервере обязательно `DEV_AUTH=false`. `BOT_TOKEN` — токен Telegram-бота, `SESSION_SECRET` — отдельный случайный ключ сессий. Не заменяйте значения работающей БД при обновлении.

Приложение слушает `127.0.0.1:7584`; существующий Caddy/Nginx должен проксировать HTTPS на этот порт с поддержкой WebSocket. БД и Redis не публикуют порты.

```sh
git pull --ff-only
make build
make status
make logs
make backup
make restore FILE=backups/bilyard-YYYYMMDDTHHMMSSZ.dump
make down
```

Обновление переводит старый backend в режим завершения матчей. Если есть активные матчи, команда останавливается до сборки: повторите её после завершения игр. Перед обновлением выполняется `pg_dump`. `make down` сохраняет Docker volumes. Восстановление останавливает приложение и заменяет данные бильярда выбранным backup; выполняйте его только при необходимости.

## Голос

LiveKit выдаёт доступ только участникам комнаты, микрофон включается отдельно. Конфигурация генерируется из `.env` командой `scripts/configure.py`. Для WebSocket можно использовать `wss://bilyard.deltavpn.me/livekit` через включённый nginx route, дополнительная DNS-запись не нужна.

Входящие порты: TCP 7881, UDP 7882, UDP 3478. Для TURN/TLS добавьте сертификат и ключ домена TURN в `infra/tls/turn.crt` и `infra/tls/turn.key`, установите `TURN_TLS=true`, `TURN_DOMAIN=bilyard.deltavpn.me`. Порт TCP 5349 должен быть доступен. Скрипт `scripts/sync-turn-cert.sh` синхронизирует сертификат конкретного домена из Caddy; запускать от root, в том числе после обновления сертификата. В сетях, разрешающих исключительно TCP 443, TURN на 5349 может быть недоступен: потребуется отдельный IP с TURN на 443 или L4-маршрутизация. HTTPS reverse proxy сам по себе не проксирует TURN.

## Telegram

Укажите домен в BotFather и Main Mini App URL `https://bilyard.deltavpn.me`, если требуется запуск по `?startapp`. `python3 scripts/configure-bot.py` устанавливает menu button, команды и описание через Bot API. Скрипт не меняет токен и не рассылает сообщения.

Бот использует polling `getUpdates`. Не запускайте второй polling/webhook-процесс с тем же токеном. Чтобы получать приглашения от друзей, игрок должен нажать `/start`. Доступны ссылки комнаты и дружбы, поиск зарегистрированных игроков, принятие/удаление заявки и блокировка.

## Локальная разработка

```sh
cp .env.example .env
# Заполнить случайные секреты, BOT_TOKEN оставить пустым,
# DEV_AUTH=true, BASE_URL=http://localhost:5173
python3 scripts/configure.py
docker compose -f compose.yaml -f compose.dev.yaml up -d postgres redis
cd backend && mvn verify && cd ..
python3 scripts/run-local.py
# В другом терминале:
cd frontend && npm ci && npm run dev
```

Открыть `http://localhost:5173/?dev=101` и отдельную вкладку `?dev=102`. Dev-вход запрещён в production. Локальная БД доступна только на 55432, Redis — 56379.

## CI

По решению владельца GitHub Actions пока не активирован. Готовый шаблон находится в `docs/ci/github-actions.yml`; для включения поместите его в `.github/workflows/ci.yml` с подходящими GitHub-правами.

## Проверки

```sh
make test
BASE_URL=http://localhost:8080 node scripts/smoke.mjs
BASE_URL=http://localhost:8080 PAIRS=50 node scripts/load.mjs
```

Smoke/load требуют изолированного backend с `DEV_AUTH=true`; не запускайте их против production. Они создают тестовые профили и матчи. Java-тесты покрывают детерминизм, быстрые столкновения, борта, лузы, неправильные удары, восьмёрку, очереди 2×2 и токены. Отчёт ручной и нагрузочной проверки — `docs/verification.md`.

## Контракты

REST: `/api/auth`, `/api/me`, `/api/rooms`, `/api/rooms/{code}/join`, `/ticket`, `/command`, `/voice`, `/api/friends`, `/api/players`, `/api/history`. Telegram `initData` проверяется HMAC и сроком до 1 часа. Сессия живёт 24 часа.

WebSocket `/ws?ticket=…`: одноразовый билет на 30 секунд, полученный по авторизованному REST. Команды `ready`, `shoot`, `place`, `next`, `leave` содержат `id` (UUID) и `seq`. Сервер отклоняет устаревшие состояния; повтор того же ID безопасен. Серверные сообщения `state`, `error`, `reaction`, `aim`, `pong`. `state` содержит `serverTime`, очередь, дедлайн и при наличии запись удара.

Правила варианта описаны в `docs/rules.md`. Физика — игровая модель с трением и вращением; прыжки и массе не реализованы. Нет платежей, ставок, рейтингового подбора и турниров.
