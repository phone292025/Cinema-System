.PHONY: up rebuild down restart ps logs backend-logs frontend-logs health config test-backend lint-frontend test-frontend build-frontend verify

up:
	docker compose up -d --no-build

rebuild:
	docker compose up --build -d

down:
	docker compose down

restart: down up

ps:
	docker compose ps

logs:
	docker compose logs -f

backend-logs:
	docker compose logs -f backend

frontend-logs:
	docker compose logs -f frontend

health:
	curl -fsS http://127.0.0.1:8080/api/actuator/health/readiness

config:
	docker compose config --quiet

test-backend:
	cd backend && ./mvnw test

lint-frontend:
	cd frontend && npm run lint

test-frontend:
	cd frontend && npm test

build-frontend:
	cd frontend && npm run build

verify: test-backend lint-frontend test-frontend build-frontend
