# ADR-004 · Búsqueda con PostgreSQL en el MVP y Elasticsearch en F3

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RF-BUS-01 a RF-BUS-08, RNF-REN-02

## Contexto

El MVP necesita búsqueda por texto con filtros, orden y paginación (RF-BUS-01 a 04) con un p95
menor a 500 ms sobre 10.000 publicaciones (RNF-REN-02). La tolerancia a errores de tipeo,
sinónimos, autocompletado y facetas (RF-BUS-06 a 08) son *Could* de la fase F3. Operar un clúster
de Elasticsearch desde el MVP agrega costo y sincronización de índices sin ser necesario.

## Decisión

- En el MVP la búsqueda usa **texto completo de PostgreSQL** (`tsvector` con diccionario
  `spanish`, índices **GIN** y `pg_trgm` para coincidencias parciales).
- El módulo `search` expone el puerto **`ListingSearch`**; el adaptador del MVP lo implementa con
  consultas parametrizadas sobre PostgreSQL.
- En F3 se agrega un adaptador de **Elasticsearch/OpenSearch** detrás del mismo puerto, alimentado
  por eventos de dominio del catálogo.

## Consecuencias

- Sin infraestructura adicional en el MVP; la búsqueda es transaccionalmente consistente.
- El rendimiento se verifica con k6 sobre datos semilla.
- Cambiar de motor no afecta a los casos de uso ni a la API, solo al adaptador.
- La relevancia de PostgreSQL es más limitada (sin sinónimos ni tolerancia a errores avanzada)
  hasta F3.
