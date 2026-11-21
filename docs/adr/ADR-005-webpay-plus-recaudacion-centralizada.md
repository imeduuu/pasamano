# ADR-005 · Webpay Plus con recaudación centralizada; Webpay Plus Mall en F3

- **Estado:** Aceptada
- **Fecha:** 2026-11-21
- **Requisitos relacionados:** RF-PAG-01 a RF-PAG-11, RN-03, RN-13, RNF-SEG-06

## Contexto

Una orden de compra agrupa pedidos de varios vendedores y se paga con **una sola transacción**
(RN-03). Transbank ofrece Webpay Plus (un código de comercio) y Webpay Plus Mall (divide una
transacción entre varios comercios). El proyecto opera solo en el **ambiente de integración**,
sin dinero real, y no hay liquidación real a vendedores (fuera de alcance).

## Decisión

- El MVP usa **Webpay Plus** con un único código de comercio: la plataforma recauda el total y
  registra un **saldo simulado por vendedor** igual al total de su pedido (RN-13).
- El módulo `payments` define el puerto **`PaymentGateway`** (crear, confirmar, consultar estado,
  reembolsar). `WebpayPlusAdapter` lo implementa con el SDK oficial de Transbank;
  `FakePaymentGateway` lo implementa en memoria para pruebas y como respaldo de la demo.
- La confirmación (*commit*) es **idempotente** por token (RF-PAG-04).
- **Webpay Plus Mall** (pago dividido por vendedor) se evalúa en F3 como otro adaptador del mismo
  puerto (RF-PAG-11).

## Consecuencias

- Integración más simple y un solo flujo de redirección; los datos de tarjeta solo se capturan
  en el formulario de Transbank (RNF-SEG-06).
- Los reembolsos parciales por pedido se piden sobre la transacción única.
- La liquidación a vendedores es contable y simulada; un paso a producción real requeriría Mall
  o un proceso de pagos a terceros.
