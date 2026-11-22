# Decisiones tomadas sin respaldo explícito en el SRS

Cuando el SRS no define algo, se tomó la opción más simple y se anotó aquí para revisarla en el
siguiente refinamiento. Cada punto puede confirmarse, cambiarse o pasar a un ADR.

## Plataforma y herramientas

| # | Tema | Decisión | Motivo |
| --- | --- | --- | --- |
| 1 | Versión de Angular | Angular 22 (activa) | Resuelta (2026-11-21): se mantiene Angular 22, según la decisión de plataforma del proyecto. |
| 2 | Imagen de MinIO | `cgr.dev/chainguard/minio` | MinIO dejó de publicar imágenes comunitarias en Docker Hub. |
| 3 | JRE del backend | JRE recortado con `jlink` sobre Alpine (252 MB) | La imagen `eclipse-temurin:21-jre-alpine` dejaba la imagen en 423 MB y RNF-POR-03 exige < 300 MB. |
| 4 | Lombok | No se usa | Records y clases simples bastan; se evita otro procesador de anotaciones. |
| 5 | Estructura de pruebas | Las pruebas espejan el paquete de la clase (`cl.trato.<modulo>...`); las IT terminan en `IT`; ArchUnit en `cl.trato.architecture` | Se reemplazaron las carpetas vacías `unit/` e `integration/` del esqueleto. |
| 6 | Cobertura de domain + application | Regla JaCoCo por paquete (cada paquete ≥ 85 %) | JaCoCo no agrega un subconjunto de paquetes; por paquete es más estricto. |
| 7 | Checkstyle | Reglas básicas propias; en pruebas se permiten métodos `snake_case`; las líneas de anotaciones no cuentan para el largo | Compatibilidad con google-java-format 2.0. |
| 8 | Spring en la capa de aplicación | Solo se permite `@Transactional` (spring-tx) | El SRS ubica las transacciones en la aplicación; ArchUnit prohíbe web, datos y seguridad. |
| 9 | Infraestructura de `shared` | Otros módulos pueden usarla (manejo de errores, seguridad base, rate limiting) | Es el núcleo compartido; la regla «no acceder a la infraestructura de otro módulo» excluye `shared`. |

## Identidad (Sprint 1)

| # | Tema | Decisión | Motivo |
| --- | --- | --- | --- |
| 10 | Nombres de estados y roles | En código en inglés: `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`; `USER`, `ADMIN`. El Javadoc indica la equivalencia con el SRS (PENDIENTE_VERIFICACION, ACTIVA, SUSPENDIDA, USUARIO) | Convención de código en inglés. |
| 11 | Nombre y nombre visible | Un solo campo `name` (2 a 60 caracteres): se ingresa al registrarse y se edita en el perfil | Minimización de datos. |
| 12 | Foto de perfil | Por ahora es una URL http(s) opcional; la subida a MinIO llega con las imágenes de publicaciones (Sprint 2) | Evita adelantar el adaptador de almacenamiento. |
| 13 | Región y comuna | Texto libre de hasta 60 caracteres, sin catálogo | No hay catálogo de regiones y comunas en el SRS. |
| 14 | Largo máximo de contraseña | 72 bytes UTF-8 | BCrypt ignora lo que viene después; además el adaptador rechaza explícitamente más de 72 bytes. |
| 15 | Lista de contraseñas comunes | 2.336 entradas de ≥ 10 caracteres (SecLists, licencia MIT) más variantes chilenas, en `backend/src/main/resources/security/common-passwords.txt` | Por debajo de 10 caracteres ya las rechaza el largo mínimo. |
| 16 | Bloqueo por intentos | Al quinto intento fallido se bloquea 15 min y el contador vuelve a 0. Se informa el bloqueo (423) aunque eso revele que la cuenta existe | CU-02 2b pide informarlo. |
| 17 | Orden de las comprobaciones al ingresar | Primero el bloqueo; luego la contraseña; solo con la contraseña correcta se informa «no verificada» (403) o «suspendida» (403) | No revelar el estado de cuentas ajenas. |
| 18 | Reenvío de verificación | Se acepta `POST /api/v1/auth/verify/resend` (responde siempre 202) | Resuelta (2026-11-21): CU-01 8a y CU-02 2c requieren el reenvío de verificación. |
| 19 | Cambio de contraseña (RF-AUT-07) | Se acepta `POST /api/v1/me/password`; revoca todos los tokens de refresco y pide ingresar de nuevo | Resuelta (2026-11-21): cerrar todas las sesiones es la opción más simple y segura. |
| 20 | Usuarios no verificados que recuperan la contraseña | Pueden hacerlo, pero siguen pendientes de verificación | El SRS no lo define. Los suspendidos no reciben enlace. |
| 21 | Token de refresco | Valor opaco de 256 bits; cookie `trato_refresh`, `Path=/api/v1/auth`; el logout revoca la familia completa | ADR-002. |
| 22 | Mensaje de registro | El mismo texto y el mismo código 202 exista o no el correo | CU-01 3b. |
| 23 | Reintentos de correo | 3 intentos en total (1 + 2 reintentos), espera de 2 s ×2 | Resuelta (2026-11-21): RNF-FIA-04 especifica 3 intentos en total. |
| 24 | Tokens en los eventos | `VerificationEmailRequested` y `PasswordResetRequested` llevan el token en claro solo en memoria; su `toString` lo oculta | El correo necesita el enlace y en la base solo se guarda el hash. Si el proceso cae antes de enviar, el usuario pide el reenvío (ADR-008). |
| 25 | Textos legales | Páginas `/terminos` y `/privacidad` breves, versión `2026-11-20` | Falta redactar los documentos definitivos (RNF-LEG-01). |

## Seguridad y operación

| # | Tema | Pendiente |
| --- | --- | --- |
| 26 | Rate limiting en memoria | Se mantiene en memoria para el MVP (ver ADR-009). | Resuelta (2026-11-21): se acepta el límite por instancia en el MVP; la evolución queda documentada en ADR-009. |
| 27 | IP del cliente | Se confía en `X-Forwarded-For` (`forward-headers-strategy: framework`). nginx lo sobrescribe con la IP real, pero si el backend se expone directo (puerto 8080) un cliente puede falsearlo. En demo, exponer solo el proxy. |
| 28 | Revocación del JWT de acceso | No existe lista negra: un JWT robado vale hasta 15 min, incluso si se suspende la cuenta (ADR-002). |
| 29 | Executor de correos | Al definir `emailExecutor`, Spring Boot deja de crear su `applicationTaskExecutor` por defecto. No afecta al MVP, pero revisarlo si se usa `@Async` en otros módulos. |
| 30 | E2E en CI | La etapa 8 (docker compose + Playwright en PR a `main`) aún no está en `ci.yml`; la prueba se ejecuta en local con `npm run e2e`. |
| 31 | Navegadores E2E | Hoy solo Chromium (escritorio y 360 px). RNF-COM-01 pide además Firefox y WebKit. |
| 32 | Slugs de categorías (RF-CAT-01) | Se normalizan a minúsculas ASCII, quitando diacríticos y sustituyendo secuencias no alfanuméricas por guiones; el índice único de PostgreSQL evita colisiones | El SRS no define la normalización; se busca URL legible y consistencia entre idiomas. |
| 33 | Auditoría administrativa de categorías (RF-ADM-05) | Cada mutación registra `adminId`, acción (`CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_DEACTIVATED`), UUID del recurso, motivo opcional e instante UTC; la tabla conserva los registros aunque se elimine al usuario | El requisito define los datos auditados, pero no el vocabulario de acciones ni las reglas de retención. |
