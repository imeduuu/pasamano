# ADR-009 · Rate limiting en memoria durante el MVP

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RF-AUT-02, RNF-ESC-01, RF-CAR-06

## Contexto

El inicio de sesión limita a **5 solicitudes por minuto** por cliente mediante un rate limiter
que mantiene su estado en memoria. Una instancia del backend puede aplicar ese límite sin
infraestructura adicional, pero el estado no se comparte entre réplicas ni persiste tras un
reinicio.

## Decisión

Se mantiene el rate limiting en memoria durante el MVP. El límite de inicio de sesión queda en
5 solicitudes por minuto por instancia.

## Consecuencias

- En un despliegue con varias réplicas, cada instancia aplica el límite por separado; el límite
  efectivo depende de la instancia que reciba cada solicitud.
- Al reiniciar una instancia se pierde el estado del rate limiter y los contadores comienzan de
  nuevo.
- El MVP no necesita operar un servicio adicional para compartir el estado del límite.

## Plan

En F2, migrar el estado del rate limiter a Redis para compartirlo entre réplicas y conservar el
límite de forma centralizada, junto con la implementación de **RF-CAR-06**.
