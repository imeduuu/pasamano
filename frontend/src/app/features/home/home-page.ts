import { Component } from '@angular/core';
import { SearchBox } from '../../shared/search-box';

@Component({
  selector: 'app-home-page',
  imports: [SearchBox],
  template: `
    <section class="hero" aria-labelledby="hero-title">
      <h1 id="hero-title">Compra y vende entre personas, sin capturas de pantalla.</h1>
      <p class="lead">
        Publica con stock, paga con Webpay y sigue tu pedido hasta que llegue. La reputación se
        construye solo con compras reales.
      </p>
      <app-search-box id="home-search" />
    </section>
  `,
  styles: `
    .hero {
      max-width: 44rem;
      padding-block: 3rem;
    }
    h1 {
      font-size: clamp(1.75rem, 4vw, 2.75rem);
      line-height: 1.15;
      margin: 0 0 1rem;
    }
    .lead {
      color: var(--color-muted);
      font-size: 1.125rem;
    }
  `,
})
export class HomePage {}
