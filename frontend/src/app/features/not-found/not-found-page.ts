import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <h1>No encontramos esta página</h1>
    <p>Puede que el enlace esté incompleto o que la página ya no exista.</p>
    <a routerLink="/">Volver al inicio</a>
  `,
})
export class NotFoundPage {}
