# Trato.

Marketplace web de compra-venta entre personas: publicar productos con stock, buscarlos, comprar
a varios vendedores en una sola compra, pagar con **Webpay Plus** (ambiente de integración de
Transbank) y calificar al vendedor.

> **Proyecto de demostración y portafolio.** Los pagos usan exclusivamente el ambiente de pruebas
> de Transbank: no se mueve dinero real.

## Estado

| Sprint | Contenido | Estado |
| --- | --- | --- |
| 0 | Fundaciones: monorepo, CI, docker compose, ADR, layout | ✅ |
| 1 | Identidad: registro, verificación, login, refresh, recuperación, perfil (HU-01 a HU-06) | ✅ |
| 2+ | Publicaciones, búsqueda, carrito, checkout, Webpay, postventa | Pendiente |

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 4.2 (Spring 7.1, Security 7.2), Spring Data JPA / Hibernate 7, Flyway |
| Base de datos | PostgreSQL 16 |
| Seguridad | JWT HS256 (Nimbus) de 15 min + token de refresco opaco rotativo en cookie HttpOnly, BCrypt(12), Bucket4j |
| API | REST `/api/v1`, OpenAPI 3 (springdoc), errores RFC 9457 (Problem Details) con `code` estable |
| Frontend | Angular 22, TypeScript 6, componentes standalone y signals |
| Pruebas | JUnit 6, AssertJ, Mockito, Testcontainers, ArchUnit, MockMvc, JaCoCo · Vitest · Playwright |
| Calidad | Spotless (google-java-format), Checkstyle, ESLint (angular-eslint), Prettier |
| Infra local | Docker Compose: PostgreSQL, MinIO, Mailpit, backend y frontend (nginx) |

## Arquitectura

Monolito modular con **arquitectura hexagonal** ([ADR-001](docs/adr/ADR-001-monolito-modular-hexagonal.md)).
Cada módulo (`identity`, `notifications`, `catalog`, …) bajo `cl.trato` tiene:

```text
<modulo>/
  domain/                 Java puro: entidades, value objects, reglas RN-xx, eventos
  application/            casos de uso y puertos (interfaces)
  infrastructure/in/      adaptadores de entrada (REST, eventos)
  infrastructure/out/     adaptadores de salida (JPA, SMTP, seguridad)
  infrastructure/config/  cableado de beans
```

Las reglas se verifican con ArchUnit en cada build: el dominio no depende de Spring, JPA ni
Jakarta; la aplicación no depende de la infraestructura; un módulo no usa la infraestructura de
otro. Los módulos se comunican por eventos de dominio después del commit
([ADR-008](docs/adr/ADR-008-eventos-de-dominio-after-commit.md)).

- Especificación: `SRS_Trato_Sprint0.pdf` (documento interno, fuera del control de versiones).
- Decisiones: [docs/adr](docs/adr) (ADR-001 a ADR-009), incluida
  [ADR-009: rate limiting en memoria](docs/adr/ADR-009-rate-limiting-en-memoria.md), y
  [docs/decisiones-pendientes.md](docs/decisiones-pendientes.md).

```text
backend/    Spring Boot (Maven Wrapper)      frontend/   Angular + nginx
docs/adr/   decisiones de arquitectura       .github/    CI y plantillas
docker-compose.yml                           .env.example
```

## Levantar todo con Docker

Requisitos: Docker Desktop (o Docker Engine con Compose v2).

```bash
cp .env.example .env            # opcional: sin .env se usan valores de desarrollo
docker compose --profile app up --build
```

| Servicio | URL |
| --- | --- |
| Aplicación | http://localhost:4200 |
| API y salud | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui |
| Mailpit (correos) | http://localhost:8025 |
| Consola MinIO | http://localhost:9001 |

Flujo para probar: crea una cuenta en `/registro`, abre el correo en Mailpit, sigue el enlace de
verificación e ingresa. Solo la infraestructura (para desarrollar con el IDE):
`docker compose up -d`.

## Desarrollo local

```bash
# Backend (JDK 21; el wrapper descarga Maven)
cd backend
./mvnw spring-boot:run          # perfil local: usa postgres y mailpit de docker compose

# Frontend (Node 24 LTS o superior)
cd frontend
npm ci
npm start                       # http://localhost:4200, con proxy /api -> localhost:8080
```

Perfiles del backend: `local` (desarrollo), `test` (pruebas, con Testcontainers) y `demo`
(logs JSON). Toda la configuración sensible llega por variables de entorno (ver `.env.example`).

## Pruebas

```bash
# Backend: formato, Checkstyle, unitarias, integración (Testcontainers), ArchUnit y JaCoCo
cd backend && ./mvnw verify

# Frontend
cd frontend
npm run format:check && npm run lint && npm run test:ci && npm run build

# E2E (con el stack levantado: docker compose --profile app up)
npx playwright install chromium
npm run e2e
```

Umbrales de cobertura (RNF-MAN-02): **85 %** de líneas en `domain` y `application`, **70 %**
global. El build falla si bajan. Las pruebas de integración necesitan Docker en ejecución.

## Licencia

[MIT](LICENSE)
