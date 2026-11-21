import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Layout principal: aviso de demostración, encabezado, contenido y pie (RNF-LEG-03). */
@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  protected readonly year = new Date().getFullYear();
}
