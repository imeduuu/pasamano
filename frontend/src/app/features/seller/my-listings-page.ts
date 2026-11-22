import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Destino de navegación para vendedores (CU-15). */
@Component({
  selector: 'app-my-listings-page',
  imports: [RouterLink],
  template: `
    <section class="auth-card" aria-labelledby="listings-title">
      <h1 id="listings-title">Mis publicaciones</h1>
      <p>Aún no tienes publicaciones.</p>
      <a class="button" routerLink="/publicar-producto">Publicar producto</a>
    </section>
  `,
})
export class MyListingsPage {}
