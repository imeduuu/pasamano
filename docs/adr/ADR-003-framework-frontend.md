# ADR-003 · Framework del frontend

- **Estado:** Aceptada: Angular
- **Fecha:** 2026-11-21 (cierra en Sprint 0)
- **Requisitos relacionados:** RNF-USA-01, RNF-USA-02, RNF-REN-03, RNF-COM-01

## Contexto

El SRS deja abiertas dos opciones: Angular (propuesta original) o React (ya dominado, con menos
curva). El criterio definido es: si las ofertas laborales objetivo piden Angular, se usa Angular;
si no, React, para concentrar el aprendizaje en el backend. El esqueleto inicial del repositorio
ya se organizó con la estructura de una aplicación Angular (`core`, `shared`, `layouts`,
`features`) y el stack del SRS (§8.3) lista «Angular (versión LTS vigente) + TypeScript».

## Decisión

Se usa **Angular 22** con TypeScript, componentes *standalone*, *signals*, el builder
`@angular/build` (esbuild/Vite) y pruebas unitarias con **Vitest**. La calidad se controla con
**ESLint** (angular-eslint, incluidas sus reglas de accesibilidad para plantillas) y **Prettier**.
Las pruebas E2E usan **Playwright**.

## Consecuencias

- Se alinea con ofertas del mercado chileno de backend Java que piden Angular en el frontend.
- Formularios reactivos tipados, router y cliente HTTP vienen integrados: menos dependencias.
- Mayor curva de aprendizaje que React; se mitiga con componentes simples y rebanadas
  verticales (riesgo R-08).
- Angular 22 es la versión activa a la fecha (la 21 está en soporte LTS); se actualizará a cada
  versión mayor siguiendo la política de soporte de Angular.
