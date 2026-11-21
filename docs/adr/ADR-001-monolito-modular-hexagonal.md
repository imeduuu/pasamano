# ADR-001 · Monolito modular con arquitectura hexagonal

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RNF-MAN-01, RNF-ESC-01, OBJ-2

## Contexto

Trato lo construye una sola persona con presupuesto cero y una capacidad de 15 a 18 SP por
sprint (SRS §2.5). El dominio tiene límites claros (identidad, catálogo, carrito, pedidos, pagos,
reseñas, notificaciones, administración), pero el tráfico esperado es el de una demo de
portafolio. Microservicios multiplicarían el costo de despliegue, observabilidad, transacciones
distribuidas y pruebas sin un beneficio real a esta escala.

## Decisión

Se construye un **monolito modular** con **arquitectura hexagonal** (puertos y adaptadores):

- Cada módulo vive bajo `cl.trato.<modulo>` con tres capas: `domain` (Java puro: entidades,
  value objects, reglas RN-xx y eventos), `application` (casos de uso y puertos) e
  `infrastructure/in|out` (adaptadores web, persistencia, correo, pasarela, etc.).
- `shared` es el núcleo compartido (`Money`, `DomainEvent`, excepciones base, manejo de errores).
- Los módulos se comunican mediante **eventos de dominio en proceso** (ver ADR-008), nunca a
  través de la infraestructura de otro módulo.
- Las reglas se verifican en CI con **ArchUnit**: el dominio no depende de Spring, JPA, Jakarta
  ni infraestructura; la aplicación no depende de infraestructura; un módulo no usa la
  infraestructura de otro.

## Consecuencias

- Un solo artefacto, un solo despliegue y transacciones ACID locales (RNF-FIA-01).
- Los límites de módulo quedan explícitos y verificados; extraer un módulo a un servicio aparte
  sigue siendo posible porque solo se acopla por eventos y puertos.
- Más archivos por caso de uso (puertos, adaptadores, mapeos) que una arquitectura en capas
  tradicional: se acepta a cambio de un dominio testeable sin Spring.
- El backend es *stateless* (JWT, ADR-002), por lo que puede ejecutarse con varias réplicas.
