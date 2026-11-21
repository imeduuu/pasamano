## Qué

<!-- Resumen del cambio. Enlaza la historia: Closes #<número> (HU-xx) -->

## Por qué

<!-- Problema que resuelve y requisitos del SRS que cubre (RF-xx, RNF-xx, RN-xx, CU-xx). -->

## Cómo probar

1.
2.

## Capturas

<!-- Si cambia la interfaz: antes / después, escritorio y móvil (360 px). -->

## Checklist (Definition of Done)

- [ ] Pruebas escritas con TDD: caso feliz y flujos alternativos relevantes.
- [ ] `./mvnw verify` y `npm run lint && npm run test:ci && npm run build` en verde.
- [ ] Cobertura sobre los umbrales (85 % dominio y aplicación, 70 % global) y sin violaciones de ArchUnit.
- [ ] Endpoints documentados en OpenAPI.
- [ ] Interfaz responsive y accesible por teclado (si aplica).
- [ ] Migración Flyway incluida (si cambia el esquema).
- [ ] CHANGELOG, README o ADR actualizados (si aplica).
- [ ] Sin secretos ni datos personales en el código, logs o capturas.
