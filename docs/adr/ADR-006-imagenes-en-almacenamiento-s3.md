# ADR-006 · Imágenes en almacenamiento compatible con S3, nunca en la base de datos

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RF-PUB-02, RN-12, RNF-REN-04, RNF-SEG-09

## Contexto

Cada publicación admite de 1 a 8 imágenes de hasta 5 MB (RN-12), con miniaturas para los listados
(RNF-REN-04). Guardar binarios en PostgreSQL agranda los respaldos, encarece las consultas y
complica servirlos con caché.

## Decisión

- Las imágenes se guardan en un **almacenamiento de objetos con API S3**: **MinIO** en local
  (servicio de `docker-compose.yml`, bucket privado `listings`) y un proveedor S3 compatible con
  plan gratuito en la demo.
- La base de datos guarda solo las **claves de objeto** (`claveObjeto`, `claveMiniatura`) y el
  orden.
- El módulo `catalog` define un puerto de almacenamiento; `MinioImageStorageAdapter` lo
  implementa.
- Antes de guardar, las imágenes se validan por contenido (*magic bytes*), se recodifican y se les
  elimina EXIF (RNF-SEG-09).

## Consecuencias

- Respaldos de base de datos livianos; las imágenes escalan por separado.
- Se depende de un servicio extra en local (MinIO) y de credenciales S3 en la demo.
- Nota de Sprint 0: MinIO dejó de publicar imágenes comunitarias en Docker Hub; el compose usa la
  imagen de MinIO de Chainguard (`cgr.dev/chainguard/minio`).
