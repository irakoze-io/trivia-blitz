# AGENTS.md

## Project Snapshot
- Monorepo with two independent apps: Spring Boot backend at repo root and Angular 21 SSR frontend in `ng-ui/`.
- Current codebase is scaffold-stage: backend has only app bootstrap + context test; frontend still uses Angular starter UI/template.
- No backend-to-frontend API wiring exists yet; treat integration as a new feature, not an existing contract.

## Architecture Map (What Exists Today)
- Backend entrypoint: `src/main/java/dev/irakodes/triviablitz/TriviaBlitzApplication.java` (`@SpringBootApplication`).
- Backend config is minimal (`src/main/resources/application.yaml` only sets `spring.application.name`).
- Backend dependencies include reactive stack + WebSocket (`spring-boot-starter-webflux`, `spring-boot-starter-websocket`, `spring-boot-starter-webclient`).
- Frontend uses standalone Angular app bootstrap (`ng-ui/src/main.ts`) with `bootstrapApplication(App, appConfig)`.
- SSR is enabled: server bootstrap in `ng-ui/src/main.server.ts`, Node/Express host in `ng-ui/src/server.ts`.
- Server prerender config catches all routes (`ng-ui/src/app/app.routes.server.ts` -> `path: '**'`, `RenderMode.Prerender`).

## Critical Workflows (Verified Here)
- Backend tests from repo root:
```bash
./gradlew test
```
- Frontend production build from `ng-ui/`:
```bash
npm run build
```
- Frontend output path after build: `ng-ui/dist/ng-ui` (browser + server bundles + prerendered routes).

## Conventions and Local Patterns
- Java package root is `dev.irakodes.triviablitz`; keep new backend code under this namespace.
- Java toolchain is pinned to 25 in `build.gradle`; avoid introducing language features requiring a different version.
- Spring Boot plugin version is `4.1.0`; dependency management is already handled by Gradle plugins in `build.gradle`.
- Lombok is configured (compile/test + annotation processors); use it only when it reduces boilerplate clearly.
- Angular uses standalone components (no NgModule patterns present).
- Global frontend styling imports Tailwind via `ng-ui/src/styles.css` (`@import "tailwindcss";`).

## Integration Guidance
- If adding APIs, primary place is Spring Boot (`src/main/java/...`); keep `ng-ui/src/server.ts` focused on SSR/static hosting unless you intentionally want Node-owned endpoints.
- If frontend consumes backend APIs, document base URL strategy early (same-origin via proxy vs separate host/port) because no convention is established yet.
- `compose.yaml` currently has no services; do not assume local infra dependencies exist.

## Key Files to Read First
- `build.gradle`
- `src/main/java/dev/irakodes/triviablitz/TriviaBlitzApplication.java`
- `src/test/java/dev/irakodes/triviablitz/TriviaBlitzApplicationTests.java`
- `ng-ui/angular.json`
- `ng-ui/package.json`
- `ng-ui/src/server.ts`
- `ng-ui/src/app/app.config.ts`
- `ng-ui/src/app/app.routes.server.ts`

