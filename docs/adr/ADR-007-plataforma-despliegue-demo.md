# ADR-007 · Plataforma de despliegue del entorno demo

- **Estado:** Pendiente (cierra en Sprint 6)
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RNF-DIS-01, RNF-SEG-03, RNF-FIA-05, RNF-POR-03, R-06

## Contexto

El MVP debe quedar desplegado en una URL pública y estable (criterio de éxito §12) con presupuesto
cero (§2.5). Las opciones son un PaaS con plan gratuito o un VPS. Hay que considerar memoria
disponible para la JVM, suspensión por inactividad (arranques en frío), PostgreSQL gestionado,
almacenamiento S3, SMTP con plan gratuito, HTTPS con HSTS y respaldos diarios con 7 días de
retención.

## Decisión

Pendiente. Se evaluará en el Sprint 6 con estos criterios:

1. Costo cero sostenible durante la evaluación del portafolio.
2. Soporte de imágenes Docker publicadas en GHCR.
3. Memoria suficiente para el backend (imagen < 300 MB, `MaxRAMPercentage`).
4. HTTPS administrado con HSTS (RNF-SEG-03).
5. PostgreSQL 16 gestionado o en el mismo host, con respaldo diario (RNF-FIA-05).
6. Variables de entorno y secretos fuera del repositorio (RNF-SEG-07).

## Consecuencias

- Hasta cerrar este ADR, el entorno de referencia es `docker compose` local.
- La imagen del backend ya se construye pensando en hosts con poca memoria (JRE recortado con
  jlink, perfil `demo` con logs JSON).
