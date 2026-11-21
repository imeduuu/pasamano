# ADR-008 · Eventos de dominio con Spring Application Events después del commit

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RF-NOT-02, RNF-FIA-04, ADR-001

## Contexto

Los módulos deben comunicarse sin acoplarse (ADR-001). Varias reacciones, como enviar correos,
registrar saldos o auditar, no deben bloquear ni revertir la operación que las origina
(RF-NOT-02), y no deben ejecutarse si esa transacción falla. Un *broker* de mensajes externo
agregaría infraestructura innecesaria para un monolito.

## Decisión

- Los agregados registran **eventos de dominio** (`DomainEvent`, en `shared.domain`); los casos de
  uso los publican mediante el puerto `DomainEventPublisher`, implementado con el
  `ApplicationEventPublisher` de Spring.
- Los suscriptores usan **`@TransactionalEventListener(phase = AFTER_COMMIT)`** y, cuando la
  reacción es lenta o externa (correo), se ejecutan con **`@Async`** y **reintentos con espera
  exponencial (3 intentos)** (RNF-FIA-04).
- El **patrón outbox** se adoptará solo si aparece la necesidad (por ejemplo, entrega garantizada
  ante caídas del proceso).

## Consecuencias

- Sin infraestructura adicional; los eventos nunca salen de una transacción revertida.
- Si el proceso se cae entre el commit y la ejecución del suscriptor, el evento se pierde (no hay
  persistencia). Para correos transaccionales se acepta: el usuario puede pedir el reenvío
  (verificación) o repetir la solicitud (recuperación de contraseña).
- Las pruebas de integración verifican que los eventos se publiquen después del commit y que los
  correos se reintenten ante fallas de SMTP.
