# Trato.

[![CI](https://github.com/imeduuu/pasamano/actions/workflows/ci.yml/badge.svg)](https://github.com/imeduuu/pasamano/actions/workflows/ci.yml)

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
| 2 | Perfil vendedor, publicaciones, imágenes y administración de categorías (HU-07 a HU-10) | Implementado |
| 3+ | Búsqueda avanzada, carrito, checkout, Webpay, postventa | Pendiente |

### Entregado en Sprint 2

- **HU-07:** activar el perfil vendedor y aceptar los términos de venta.
- **HU-08:** crear publicaciones con categoría, precio, stock, condición y ubicación; guardar borradores y publicar.
- **HU-09:** cargar, procesar, ordenar y eliminar imágenes en MinIO; seleccionar imagen principal y generar miniaturas.
- **HU-10:** consultar, editar, pausar y eliminar publicaciones propias con control de concurrencia; exponer el detalle activo y el perfil del vendedor.
- Administración de categorías con auditoría y catálogo oficial de regiones y comunas de Chile.

**Capturas de pantalla:** TODO: agregar capturas de inicio, detalle de publicación, gestión del vendedor y administración de categorías.

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

## Problemas resueltos

- **BCrypt y contraseñas largas:** BCrypt solo considera 72 bytes; aceptar más podía hacer que
  contraseñas con el mismo prefijo coincidieran. `PasswordPolicy` rechaza más de 72 bytes UTF-8 y
  `BCryptPasswordHasher.matches` no acepta contraseñas que exceden ese límite.
- **Suplantación de IP y rate limit:** el login calcula el límite con `getRemoteAddr()`, que recibe
  la IP reenviada por nginx. nginx ahora sobrescribe `X-Forwarded-For` con `$remote_addr`, así que
  un valor enviado por el cliente no permite cambiar la IP usada para el límite.
- **Spotless en Windows:** las diferencias CRLF/LF hacían fallar el formato; `.gitattributes`
  normaliza los archivos del repositorio a LF y Spotless está configurado con `lineEndings` UNIX.

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

En el perfil `demo` configura obligatoriamente estas variables antes de iniciar el backend:

| Variable | Requisito |
| --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USERNAME`, `DB_PASSWORD` | Credenciales de PostgreSQL |
| `JWT_SECRET` | Clave aleatoria propia de al menos 32 bytes; no uses el valor de desarrollo de Docker Compose. |
| `REFRESH_COOKIE_SECURE` | `true`; el backend rechaza `false` fuera de `local` y `test`. |
| `S3_ENDPOINT` | Endpoint HTTPS del almacenamiento compatible con S3 |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | Credenciales del almacenamiento S3 |
| `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS` | URL pública del frontend; configura los orígenes reales del despliegue. |

Genera un secreto, por ejemplo, con `openssl rand -base64 48`. Las validaciones de inicio
protegen JWT y la cookie de refresco según las decisiones [#27](docs/decisiones-pendientes.md)
y [#51](docs/decisiones-pendientes.md).

### Datos semilla locales

Al iniciar con el perfil `local` se crean de forma idempotente categorías iniciales de dos niveles
(Tecnología, Hogar, Deportes y Moda, con sus subcategorías). No se generan publicaciones salvo que
se configure una cantidad. Para preparar 10.000 publicaciones falsas para las pruebas de búsqueda
del Sprint 3, levanta primero la infraestructura local y ejecuta desde `backend/`:

```powershell
docker compose -f ..\docker-compose.yml up -d
$env:SEED_LISTINGS = "10000"
.\mvnw.cmd spring-boot:run
Remove-Item Env:SEED_LISTINGS
```

También puedes pasar `--trato.seed.listings=10000` a la aplicación. Datafaker crea títulos,
descripciones, vendedores, precios y ubicaciones; cada publicación queda `ACTIVA` y se asocia a una
categoría hoja. Las imágenes son marcadores SVG generados en el backend y guardados en MinIO local;
no se descarga contenido externo. El valor predeterminado es `0`, y los datos semilla solo se
habilitan con el perfil `local`.

### Prueba de carga de búsqueda

Requiere Docker Compose, Docker y el backend local con las 10.000 publicaciones del seed de la
decisión [#43](docs/decisiones-pendientes.md). Con los servicios de infraestructura levantados,
inicia el backend desde `backend/`:

```powershell
docker compose up -d
$env:SEED_LISTINGS = "10000"
$env:API_RATE_LIMIT_PER_MINUTE = "100000"
.\mvnw.cmd spring-boot:run
```

En otra terminal PowerShell, desde la raíz del repositorio, ejecuta k6 en Docker (sin instalarlo):

```powershell
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 -v "${PWD}/perf:/scripts:ro" grafana/k6 run /scripts/search.js
```

El escenario simula 50 usuarios durante un minuto con consultas variadas por texto, precio,
condición, región, orden y página. El umbral de aceptación para `http_req_duration` es
**p95 < 500 ms** (RNF-REN-02); k6 devuelve un código distinto de cero si se incumple. La métrica
incluye el tiempo HTTP observado por el generador, incluido el transporte de red local.
El límite de peticiones local se eleva durante la prueba para que 50 usuarios desde una sola IP
no midan el limitador global en lugar de la búsqueda.

La corrida local realizada con 10.000 publicaciones y 50 usuarios obtuvo **p95 = 683 ms**;
no alcanzó el umbral. `EXPLAIN ANALYZE` mostró que el filtro por región y condición leía 646
bloques y tardaba 10,6 ms; el índice compuesto añadido redujo ese plan a 4,4 ms. La latencia
global bajo concurrencia sigue por encima del objetivo y debe volver a medirse en el entorno de
referencia de rendimiento.

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
