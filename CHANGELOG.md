# Registro de cambios

## Sprint 2 — 2026-11-23

### Añadido

- Activación de perfil vendedor y aceptación de términos de venta (HU-07).
- Creación, edición, publicación, pausa y eliminación lógica de publicaciones; control de concurrencia y detalle público (HU-08/HU-10).
- Carga, procesamiento, ordenamiento y eliminación de imágenes de publicaciones en MinIO, con miniaturas e imagen principal (HU-09).
- Administración auditada de categorías, catálogo oficial de regiones y comunas, y datos semilla locales para probar la búsqueda.
- Pruebas E2E del ciclo de publicación con imágenes, en Chromium, Firefox y WebKit para escritorio y móvil.

### Cambiado

- Se amplió el límite de solicitudes de la API usado por la suite E2E.

### Corregido

- Se amplió el tiempo de espera para recibir el correo de verificación durante las pruebas E2E.
- Se corrigió el acceso a miniaturas en la navegación por teclado del detalle de publicación, que ESLint rechazaba por el salto de línea antes del índice.
- Se ajustó la cookie de refresco del stack HTTP local para que WebKit acepte y restaure la sesión.

### Seguridad

- Compose desactiva `Secure` por defecto para la cookie de refresco en HTTP local; los entornos HTTPS deben habilitarlo.

## Sprint 1 — 2026-11-21

### Añadido

- Estructura inicial del repositorio, backend Spring Boot con base compartida, frontend Angular y entorno local con Docker Compose.
- Registro de cuentas, verificación de correo, autenticación y recuperación de contraseña, con entrega de correos a través de Mailpit.
- Pantallas de registro, verificación, inicio de sesión, recuperación de contraseña y perfil; servicio de sesión, interceptor HTTP, rutas protegidas y utilidades accesibles para formularios.
- Flujo E2E de registro, verificación e inicio de sesión con Mailpit; ejecución en Chromium, Firefox y WebKit, en escritorio y móvil.
- Workflow de CI para backend y frontend, y ejecución de Playwright en pull requests y `main`.
- Decisiones iniciales de arquitectura, decisiones pendientes y ADR sobre rate limiting en memoria.

### Cambiado

- Se documentaron en el README el estado del proyecto y las decisiones resueltas.

### Corregido

- Se corrigieron el rechazo de contraseñas de más de 72 bytes al compararlas con BCrypt, el cálculo de vigencia de tokens desde la emisión de la sesión y la transaccionalidad de los adaptadores de repositorio de tokens.
- Se ajustó la prueba del tipo de contenido del rate limit para aceptar el parámetro `charset`.

### Seguridad

- Se bloqueó la suplantación de `X-Forwarded-For` en el proxy de Nginx.
- Se agregó una prueba para verificar que los eventos de dominio no expongan tokens.
