# Spring Boot Boilerplate (Java 25 + Spring Boot 4.0.5)

A production-ready starter template for REST APIs using:

- Java 25
- Spring Boot 4.0.5
- Spring Security + JWT
- PostgreSQL
- Flyway migrations
- Swagger/OpenAPI (springdoc)

## 1. Use As A Boilerplate

1. Click **Use this template** on GitHub.
2. Create your new repository from this template.
3. Clone your new repository.

```bash
git clone <your-new-repo-url>
cd <your-new-repo-folder>
```

## 2. Requirements

- Java 25
- Maven 3.9+
- Docker (optional, for local PostgreSQL)

## 3. Environment Setup

This project uses `.env` for both Spring Boot and Docker Compose.
It is mandatory to have a PostgreSQL database running and reachable using the `.env` connection values before starting the API.
If the database is not available, application startup will fail.

```bash
cp .env.example .env
```

Main variables:

- `DB_NAME`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `APP_PUBLIC_BASE_URL` (public API domain, used in OpenAPI server URL)
- `JWT_SECRET` (base64)
- `JWT_EXPIRATION_MINUTES`
- `JWT_ISSUER`
- `SWAGGER_ENABLED` (`true` for dev, usually `false` in prod)
- `SWAGGER_UI_PATH` (default `/swagger-ui.html`)

## 4. Run PostgreSQL (Recommended)

```bash
docker compose up -d postgres
```

## 5. Run Full Stack With Docker (API + PostgreSQL)

```bash
docker compose up --build -d
```

## 6. Run The API Locally (Without Docker for API)

```bash
mvn spring-boot:run
```

Spring Boot automatically reads `.env` from the project root.

## 7. Swagger / OpenAPI

- http://localhost:8080/api/swagger-ui.html
- Swagger UI: `${APP_PUBLIC_BASE_URL}${SWAGGER_UI_PATH}`
- OpenAPI JSON: `${APP_PUBLIC_BASE_URL}/v3/api-docs`
- OpenAPI `servers` URL is taken from `.env` variable `APP_PUBLIC_BASE_URL`.

Production recommendation:
- Set `APP_PUBLIC_BASE_URL` to your real public domain (example: `https://api.yourcompany.com`)
- Set `SWAGGER_ENABLED=false` unless you explicitly want docs exposed in production.

## 8. Auth Flow

### Register

`POST /api/v1/auth/register`

```json
{
  "email": "user@example.com",
  "password": "strongPassword123"
}
```

### Login

`POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "strongPassword123"
}
```

Use returned `accessToken` as:

```text
Authorization: Bearer <token>
```

### Protected Examples

- `GET /api/v1/me` (authenticated)
- `GET /api/v1/admin/ping` (ADMIN role)

## 9. Database Migration

Flyway runs automatically on startup.
- JPA is configured with `ddl-auto=update` for smoother local bootstrap in this template.
- For production hardening, switch to `ddl-auto=validate`.
- Initial migration file included: `src/main/resources/db/migration/V1__create_users_table.sql`
- New migration files are created manually as SQL files in `src/main/resources/db/migration` using `V{number}__{description}.sql`.

## 10. Seed users SQL (for local development)

The template includes a common `app_users` table for JWT projects.
Use this SQL to quickly seed users in PostgreSQL:

```sql
INSERT INTO app_users (id, email, password, role, created_at)
VALUES (gen_random_uuid(), 'admin@example.com', '$2a$10$aTjd/2.qgbPSG/jIrmtWY.xoamJAaTSE2eIQHTVdzS337ImaV3SQG', 'ADMIN', now()),
       (gen_random_uuid(), 'user1@example.com', '$2a$10$aTjd/2.qgbPSG/jIrmtWY.xoamJAaTSE2eIQHTVdzS337ImaV3SQG', 'USER', now()),
       (gen_random_uuid(), 'user2@example.com', '$2a$10$aTjd/2.qgbPSG/jIrmtWY.xoamJAaTSE2eIQHTVdzS337ImaV3SQG', 'USER', now())
ON CONFLICT (email) DO NOTHING;
```

Notes:
- The hash above is a BCrypt hash for password: `password`.
- `role` must match enum values in `Role` (`USER`, `ADMIN`).

## 11. Common User Table (boilerplate default)

We include a user table by default because it is a very common starting point in JWT-based backends.

You can modify it here:
- Entity: `src/main/java/com/example/template/domain/AppUser.java`
- Role enum: `src/main/java/com/example/template/domain/Role.java`
- Repository: `src/main/java/com/example/template/domain/AppUserRepository.java`
- Initial SQL: `src/main/resources/db/migration/V1__create_users_table.sql`
- Auth logic: `src/main/java/com/example/template/auth/AuthService.java`

If you change table/column names, update both the entity and migration files.

## 12. Useful Commands

### Run tests

```bash
mvn test
```

### Run full verification

```bash
mvn clean verify
```

### Run API locally

```bash
mvn spring-boot:run
```

### Run TMDB movie sync manually

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--sync-tmdb-movies --server.port=0"
```

The automatic sync runs every Sunday at 11:00 PM America/Lima by default.
It checks TMDB `now_playing` movies released from the run date back to `TMDB_RELEASE_LOOKBACK_DAYS` days ago.
The default lookback is 7 days, so each weekly run covers the current week; it is capped by `TMDB_MAX_RELEASE_LOOKBACK_DAYS` at 21 days.
Full TMDB details are downloaded only for movies that are missing locally; existing movies are only activated/deactivated from current availability.

With Docker Compose:

```bash
docker compose run --rm api --sync-tmdb-movies --server.port=0
```

### Run API + DB with Docker Compose

```bash
docker compose up --build -d
```

## 13. Suggested First Boilerplate Customizations

1. Change `groupId`, `artifactId`, and package name (`com.example.template`).
2. Replace auth/domain modules with your business modules.
3. Add role/permission model that matches your domain.
4. Add integration tests with Testcontainers.
5. Configure CI/CD and deployment profile-specific configs.

## 14. Project Structure

```text
src/main/java/com/example/template
├── auth
├── common
├── config
├── domain
└── security
```
