# Decisiones tomadas sin respaldo explícito en el SRS

Cuando el SRS no define algo, se tomó la opción más simple y se anotó aquí para revisarla en el
siguiente refinamiento. Cada punto puede confirmarse, cambiarse o pasar a un ADR.

## Plataforma y herramientas

| # | Tema | Decisión | Motivo | Estado |
| --- | --- | --- | --- | --- |
| 1 | Versión de Angular | Angular 22 (activa) | Resuelta (2026-11-21): se mantiene Angular 22, según la decisión de plataforma del proyecto. | Resuelta |
| 2 | Imagen de MinIO | `cgr.dev/chainguard/minio` | MinIO dejó de publicar imágenes comunitarias en Docker Hub. | Resuelta |
| 3 | JRE del backend | JRE recortado con `jlink` sobre Alpine (252 MB) | La imagen `eclipse-temurin:21-jre-alpine` dejaba la imagen en 423 MB y RNF-POR-03 exige < 300 MB. | Resuelta |
| 4 | Lombok | No se usa | Records y clases simples bastan; se evita otro procesador de anotaciones. | Resuelta |
| 5 | Estructura de pruebas | Las pruebas espejan el paquete de la clase (`cl.trato.<modulo>...`); las IT terminan en `IT`; ArchUnit en `cl.trato.architecture` | Se reemplazaron las carpetas vacías `unit/` e `integration/` del esqueleto. | Resuelta |
| 6 | Cobertura de domain + application | Regla JaCoCo por paquete (cada paquete ≥ 85 %) | JaCoCo no agrega un subconjunto de paquetes; por paquete es más estricto. | Resuelta |
| 7 | Checkstyle | Reglas básicas propias; en pruebas se permiten métodos `snake_case`; las líneas de anotaciones no cuentan para el largo | Compatibilidad con google-java-format 2.0. | Resuelta |
| 8 | Spring en la capa de aplicación | Solo se permite `@Transactional` (spring-tx) | El SRS ubica las transacciones en la aplicación; ArchUnit prohíbe web, datos y seguridad. | Resuelta |
| 9 | Infraestructura de `shared` | Otros módulos pueden usarla (manejo de errores, seguridad base, rate limiting) | Es el núcleo compartido; la regla «no acceder a la infraestructura de otro módulo» excluye `shared`. | Resuelta |

## Identidad (Sprint 1)

| # | Tema | Decisión | Motivo | Estado |
| --- | --- | --- | --- | --- |
| 10 | Nombres de estados y roles | En código en inglés: `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`; `USER`, `ADMIN`. El Javadoc indica la equivalencia con el SRS (PENDIENTE_VERIFICACION, ACTIVA, SUSPENDIDA, USUARIO) | Convención de código en inglés. | Resuelta |
| 11 | Nombre y nombre visible | Un solo campo `name` (2 a 60 caracteres): se ingresa al registrarse y se edita en el perfil | Minimización de datos. | Resuelta |
| 12 | Foto de perfil | Por ahora es una URL http(s) opcional; la subida a MinIO llega con las imágenes de publicaciones (Sprint 2) | Evita adelantar el adaptador de almacenamiento. | Aceptada como deuda |
| 13 | Región y comuna del perfil (RF-USR-01) | El perfil guarda códigos CUT de región (2 dígitos) y comuna (5 dígitos); ambos son opcionales y se borran juntos. La comuna debe pertenecer a la región según la regla del módulo `geo`, consumida mediante un puerto de aplicación. La API devuelve los códigos; la interfaz usa los selectores región → comuna del formulario de publicación. La migración V12 asigna nombres existentes tras normalizar mayúsculas, espacios y tildes; los nombres sin coincidencia exacta quedan en `NULL`. | Reutiliza el catálogo oficial de la #39 sin acoplar identidad a la infraestructura de geo y evita conservar ubicaciones escritas fuera del catálogo. | Resuelta |
| 14 | Largo máximo de contraseña | 72 bytes UTF-8 | BCrypt ignora lo que viene después; además el adaptador rechaza explícitamente más de 72 bytes. | Resuelta |
| 15 | Lista de contraseñas comunes | 2.336 entradas de ≥ 10 caracteres (SecLists, licencia MIT) más variantes chilenas, en `backend/src/main/resources/security/common-passwords.txt` | Por debajo de 10 caracteres ya las rechaza el largo mínimo. | Resuelta |
| 16 | Bloqueo por intentos | Al quinto intento fallido se bloquea 15 min y el contador vuelve a 0. Se informa el bloqueo (423) aunque eso revele que la cuenta existe | CU-02 2b pide informarlo. | Resuelta |
| 17 | Orden de las comprobaciones al ingresar | Primero el bloqueo; luego la contraseña; solo con la contraseña correcta se informa «no verificada» (403) o «suspendida» (403) | No revelar el estado de cuentas ajenas. | Resuelta |
| 18 | Reenvío de verificación | Se acepta `POST /api/v1/auth/verify/resend` (responde siempre 202) | Resuelta (2026-11-21): CU-01 8a y CU-02 2c requieren el reenvío de verificación. | Resuelta |
| 19 | Cambio de contraseña (RF-AUT-07) | Se acepta `POST /api/v1/me/password`; revoca todos los tokens de refresco y pide ingresar de nuevo | Resuelta (2026-11-21): cerrar todas las sesiones es la opción más simple y segura. | Resuelta |
| 20 | Usuarios no verificados que recuperan la contraseña | Pueden hacerlo, pero siguen pendientes de verificación | El SRS no lo define. Los suspendidos no reciben enlace. | Resuelta |
| 21 | Token de refresco | Valor opaco de 256 bits; cookie `trato_refresh`, `Path=/api/v1/auth`; el logout revoca la familia completa | ADR-002. | Resuelta |
| 22 | Mensaje de registro | El mismo texto y el mismo código 202 exista o no el correo | CU-01 3b. | Resuelta |
| 23 | Reintentos de correo | 3 intentos en total (1 + 2 reintentos), espera de 2 s ×2 | Resuelta (2026-11-21): RNF-FIA-04 especifica 3 intentos en total. | Resuelta |
| 24 | Tokens en los eventos | `VerificationEmailRequested` y `PasswordResetRequested` llevan el token en claro solo en memoria; su `toString` lo oculta | El correo necesita el enlace y en la base solo se guarda el hash. Si el proceso cae antes de enviar, el usuario pide el reenvío (ADR-008). | Resuelta |
| 25 | Textos legales | Páginas `/terminos` y `/privacidad` breves, versión `2026-11-20` | Falta redactar los documentos definitivos (RNF-LEG-01). | Abierta |

## Seguridad y operación

| # | Tema | Decisión | Motivo | Estado |
| --- | --- | --- | --- | --- |
| 26 | Rate limiting en memoria | Se mantiene en memoria para el MVP (ver ADR-009). | Se acepta el límite por instancia en el MVP; la evolución queda documentada en ADR-009. | Resuelta |
| 27 | IP del cliente | Se confía en `X-Forwarded-For` (`forward-headers-strategy: framework`); nginx lo sobrescribe con la IP real. | Si el backend se expone directamente (puerto 8080), un cliente puede falsearlo. En demo se expone solo el proxy. | Aceptada como deuda |
| 28 | Revocación del JWT de acceso | No existe lista negra: un JWT robado vale hasta 15 min, incluso si se suspende la cuenta (ADR-002). | Se acepta la vigencia máxima de 15 minutos definida para el token de acceso. | Aceptada como deuda |
| 29 | Executor de correos | Al definir `emailExecutor`, Spring Boot deja de crear su `applicationTaskExecutor` por defecto. | No afecta al MVP; revisar si se usa `@Async` en otros módulos. | Abierta |
| 30 | E2E en CI | El job E2E existe en `.github/workflows/ci.yml` (commit `21c825d`). | La canalización CI ejecuta Docker Compose y Playwright. | Resuelta |
| 31 | Navegadores E2E | Reemplazada por la decisión #47, que registra Chromium, Firefox y WebKit en escritorio y viewport de 360 px. | La matriz actual queda documentada en la #47. | Resuelta |
| 32 | Slugs de categorías (RF-CAT-01) | Se normalizan a minúsculas ASCII, quitando diacríticos y sustituyendo secuencias no alfanuméricas por guiones; el índice único de PostgreSQL evita colisiones | El SRS no define la normalización; se busca URL legible y consistencia entre idiomas. | Resuelta |
| 33 | Auditoría administrativa de categorías (RF-ADM-05) | Cada mutación registra `adminId`, acción (`CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_DEACTIVATED`), UUID del recurso, motivo opcional e instante UTC; la tabla conserva los registros aunque se elimine al usuario | El requisito define los datos auditados, pero no el vocabulario de acciones ni las reglas de retención. | Resuelta |
| 34 | Largo de la referencia de imagen de publicaciones (ADR-006) | Se limita a 500 caracteres en `listing_images.image_reference` | El ADR define almacenar claves de objeto y orden, pero no su largo máximo; 500 cubre claves S3/MinIO sin guardar URL firmadas. | Resuelta |
| 35 | Vigencia de URL de imágenes (ADR-006) | Las URL prefirmadas de lectura vencen en 15 minutos, configurable con `S3_URL_EXPIRATION` | El bucket sigue privado y la URL temporal permite entregar la imagen sin exponer credenciales. | Resuelta |
| 36 | Formato de salida de imágenes procesadas (RN-12, RNF-SEG-09, RNF-REN-04) | Reencodificar originales y miniaturas en JPEG; limitar la miniatura a 400 px en su lado mayor | ImageIO ofrece codificación JPEG estándar y al recodificar elimina EXIF. Se acepta WebP de entrada mediante `webp-imageio`, pero su codificador no se adopta hasta validar su fiabilidad. | Resuelta |

| # | Tema | Decisión | Motivo | Estado |
| --- | --- | --- | --- | --- |
| 37 | Indicador de perfil vendedor en `/me` (CU-15) | Incluir `seller` en la respuesta del perfil y consultarlo desde el repositorio de perfiles vendedores | El encabezado necesita distinguir vendedores de usuarios sin perfil al recuperar o actualizar la sesión. | Resuelta |
| 38 | Orden de las regiones en `GET /api/v1/regions` | Columna `regions.sort_order` de norte a sur (Arica y Parinacota → Magallanes); las comunas se ordenan alfabéticamente con colación `es-CL` | El SRS no define el orden. El orden por código CUT deja Arica (15) y Ñuble (16) al final, lo que confunde al elegir región. | Resuelta |
| 39 | Catálogo de regiones y comunas (RF-PUB-01, RF-USR-04) | Módulo `geo` con tablas `regions` y `communes` (migración V10) cargadas desde el CUT 2018 de SUBDERE (`CUT_2018_v04.xls`, Decreto Exento N° 1.115/2018): 16 regiones y 346 comunas; fuente descargada y verificada el 2026-11-22. Publicaciones validan y guardan códigos CUT de región y comuna. | El SRS pide una región y comuna de origen. Se usan los códigos oficiales como identificadores estables y se valida la pertenencia con el catálogo existente. | Resuelta |
| 40 | ETag de publicaciones (HU-10, RF-PUB-04/06) | El ETag es la versión JPA entre comillas (`"0"`); PATCH y DELETE requieren `If-Match`. Una cabecera ausente o mal formada responde 412 `INVALID_IF_MATCH`; una versión obsoleta responde 409 `CONCURRENT_MODIFICATION`. | El SRS exige control de versión y los códigos HTTP 409/412 para sus fallas, sin definir la codificación HTTP del ETag. | Resuelta |
| 41 | Clave de miniatura en detalle de publicaciones (RF-PUB-08, RF-BUS-05) | Guardar la clave de miniatura en `listing_images.thumbnail_reference` y generar su URL prefirmada desde ese valor. Una migración completa las filas existentes con la convención `-thumb` antes de la extensión. | La carga de imágenes ya conoce ambas claves; persistirlas permite generar URL correctas aunque no compartan una convención de nombres. | Resuelta |
| 42 | Contrato HTTP de imágenes (HU-09, RF-PUB-02) | El multipart usa el campo `image`; `PUT .../order` recibe `{ "imageIds": [...] }`; borrar y reordenar responden 204. El primer ID del arreglo queda como imagen principal. Las posiciones 8–15 se usan solo temporalmente dentro de una transacción al reordenar o compactar. | El SRS no define nombres del campo multipart ni el cuerpo de orden; se elige un contrato directo que usa los UUID persistidos de `listing_images`. | Resuelta |
| 43 | Datos de prueba del perfil local | Un inicializador de infraestructura con JDBC inserta categorías de dos niveles y, bajo `trato.seed.listings`, crea vendedores/publicaciones de prueba; Datafaker aporta los datos y dos SVG generados localmente se guardan en MinIO. La cantidad predeterminada es 0. | Son datos de desarrollo para preparar la búsqueda del Sprint 3; JDBC evita convertir el proceso de seed en un nuevo caso de uso de catálogo y el perfil local impide ejecutarlo en demo/test. | Resuelta |
| 44 | Publicar desde CU-03 | El formulario guarda primero el borrador y luego invoca la transición `publish`; el backend mantiene la regla RN-12 de exigir al menos una imagen. | El alcance de CU-03 pide datos del producto y los botones de borrador/publicación, pero no incluye carga de imágenes. Se muestra el Problem Details devuelto si falta la imagen; la carga corresponde a CU-03 y RF-PUB-02. | Resuelta |
| 45 | Límite y formatos de imágenes en la interfaz (HU-09, RF-PUB-02, RN-12) | Aceptar JPG, PNG y WebP hasta 5 MiB por archivo y 8 archivos; mostrar progreso por archivo y usar la primera posición como principal. | Coincide con ImageProcessorAdapter y ManageListingImages; la API no publica el límite en un endpoint de metadatos. | Resuelta |
| 46 | ID del vendedor en el detalle público (RF-BUS-05) | Incluir `sellerId` en `GET /api/v1/listings/{id}` para enlazar al perfil público `/vendedores/{id}`. | El detalle devolvía el nombre de tienda pero no una referencia estable para construir el enlace al perfil existente. | Resuelta |


## Cierre de Sprint 2 (2026-11-23)

HU-07, HU-08, HU-09 y HU-10 están implementadas. Las pruebas de backend, frontend y los flujos E2E de navegador cubren los recorridos incluidos en este incremento. La administración de categorías y el catálogo de regiones/comunas también quedaron incorporados; no cambian el número de HU registrado en las pruebas de Sprint 2.

| # | Tema | Decisión | Motivo | Estado |
| --- | --- | --- | --- | --- |
| 47 | Matriz de navegadores E2E (RNF-COM-01, RNF-USA-01) | Chromium, Firefox y WebKit, en escritorio y viewport de 360 px; 18 casos en la configuración actual. | Se valida compatibilidad de los flujos de identidad y publicación en los navegadores y tamaños requeridos. | Resuelta |
| 48 | Capturas de producto | Pendiente: incorporar capturas de inicio, detalle de publicación, gestión de vendedor y categorías al README. | No se generaron capturas durante el cierre; el README conserva los TODO explícitos. | Abierta |
| 49 | E2E en CI | Resuelta por el job E2E de `.github/workflows/ci.yml` (commit `21c825d`). | El workflow incorpora Docker Compose y Playwright a la canalización CI. | Resuelta |
| 50 | Foto de perfil del vendedor | Se mantiene URL HTTP(S) opcional; la carga de foto queda pendiente para un incremento futuro. | La carga de Sprint 2 cubre las imágenes de publicaciones, no las fotos de perfil. | Aceptada como deuda |
| 51 | Cookie Secure en Compose local | `REFRESH_COOKIE_SECURE=false` por defecto para HTTP local; habilitar `true` en entornos HTTPS. | WebKit no conserva cookies Secure sobre HTTP localhost y no restaura la sesión; con este valor la matriz E2E puede validar renovación real y el despliegue seguro mantiene Secure habilitado. | Resuelta |

| 52 | Fecha de publicaciÃ³n para bÃºsqueda (HU-12, ADR-004) | Agregar `listings.published_at` nullable e indexarla; su escritura al publicar queda a cargo del flujo de publicaciÃ³n. | El esquema no guardaba esta fecha y asignarla retroactivamente habrÃ­a inventado fechas para borradores y publicaciones antiguas. | Abierta |

| 53 | Normalización de criterios de búsqueda (CU-04) | Cuando el precio mínimo supera al máximo, se eliminan ambos límites; una página negativa se normaliza a 0 sin agregar un ajuste. | CU-04 disponible indica que deben aplicarse valores por defecto e informarse los ajustes, pero no determina si se conserva uno de los límites ni el tratamiento de una página negativa. | Resuelta |

## Abiertas

- **#25:** Textos legales definitivos (RNF-LEG-01).
- **#29:** Revisar el executor de correos si se usa `@Async` en otros módulos.
- **#48:** Incorporar capturas de producto al README.
