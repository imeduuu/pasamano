# ADR-002 · Autenticación stateless con JWT corto y token de refresco rotativo

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RF-AUT-03, RF-AUT-04, RF-AUT-05, RNF-SEG-02, RNF-ESC-01

## Contexto

El backend debe poder ejecutarse con varias instancias sin sesión en memoria (RNF-ESC-01). La SPA
necesita autenticarse en cada petición y mantener la sesión sin exponer credenciales de larga
duración a JavaScript (XSS). Se requiere poder cerrar sesión y detectar el robo de tokens.

## Decisión

- **Token de acceso:** JWT firmado (HMAC-SHA256, clave desde la variable de entorno
  `JWT_SECRET`), vigencia de **15 minutos**, enviado como `Authorization: Bearer`. El frontend lo
  guarda **solo en memoria**.
- **Token de refresco:** valor opaco aleatorio de 256 bits, vigencia de **7 días**, entregado en
  una cookie `HttpOnly; Secure; SameSite=Strict` con `Path=/api/v1/auth`. En la base de datos se
  guarda **solo su hash** (SHA-256).
- **Rotación:** cada uso de `/auth/refresh` invalida el token presentado y emite uno nuevo de la
  misma **familia**. Si llega un token ya usado o revocado (**reutilización**), se revoca la
  familia completa y se obliga a iniciar sesión de nuevo.
- **Cierre de sesión:** revoca el token de refresco vigente y borra la cookie. Restablecer la
  contraseña revoca todos los tokens de refresco del usuario.
- La validación del JWT usa Spring Security Resource Server (Nimbus JOSE).

## Consecuencias

- La API no guarda sesión y escala horizontalmente.
- Un token de acceso robado vale como máximo 15 minutos; el robo de un token de refresco se
  detecta al primer reuso.
- Revocar un token de acceso antes de que expire no es posible sin una lista negra; se acepta
  por la vigencia corta. La suspensión de un usuario revoca sus tokens de refresco.
- La cookie `SameSite=Strict` exige que el frontend y la API compartan sitio: en local y demo la
  API se sirve detrás del mismo origen (proxy de nginx o del servidor de desarrollo de Angular).
