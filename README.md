# Trato

Marketplace web de compra-venta entre personas: monorepo, backend Java con arquitectura hexagonal, frontend Angular
y documentación e infraestructura separadas.

## Estructura

```text
backend/
  src/main/java/cl/trato/
    shared/
    identity/ catalog/ search/ cart/ ordering/ payments/
    reviews/ messaging/ notifications/ admin/
    <modulo>/{domain,application,infrastructure}
  src/main/resources/db/migration/
  src/test/java/cl/trato/{unit,integration,architecture}/
frontend/
  src/app/{core,shared,layouts,features}/
  e2e/
docs/
  adr/ api/ diagrams/ process/ requirements/ ux/
.github/
  ISSUE_TEMPLATE/ workflows/
```

Cada módulo del backend mantiene sus reglas en `domain`, sus casos de uso en
`application` y sus adaptadores en `infrastructure`. Los módulos de fase futura
(Redis, Elasticsearch, WebSocket y Mercado Pago) tienen su espacio previsto, pero
no se implementan en este esqueleto.

`SRS_Trato_Sprint0.pdf` permanece en la raíz como referencia del proyecto.

Este commit inicial contiene únicamente la organización de carpetas. Los
proyectos ejecutables, dependencias, migraciones y pipelines se incorporarán en
sus siguientes entregables.
