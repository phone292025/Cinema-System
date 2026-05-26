.PHONY: up rebuild down restart ps logs backend-logs frontend-logs test-backend lint-frontend build-frontend verify

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

test-backend:
	cd backend && ./mvnw test

lint-frontend:
	cd frontend && npm run lint

build-frontend:
	cd frontend && npm run build

verify: test-backend lint-frontend build-frontend
