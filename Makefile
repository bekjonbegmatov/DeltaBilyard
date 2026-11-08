.PHONY: build logs status backup restore down test
build:
	bash scripts/deploy.sh
logs:
	docker compose logs --tail=100 -f
status:
	docker compose ps
backup:
	bash scripts/backup.sh
restore:
	FILE="$(FILE)" bash scripts/restore.sh
down:
	docker compose down
test:
	cd backend && mvn verify
	cd frontend && npm ci && npm run build && npm test
