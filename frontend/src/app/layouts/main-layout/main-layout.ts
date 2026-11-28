import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SearchBox } from '../../shared/search-box';

/** Layout principal: aviso de demostración, encabezado, contenido y pie (RNF-LEG-03). */
@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, SearchBox],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly year = new Date().getFullYear();

  logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigate(['/']));
  }
}
