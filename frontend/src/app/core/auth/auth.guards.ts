import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Rutas protegidas: sin sesión, lleva al login recordando a dónde quería ir el usuario. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return true;
  }
  return inject(Router).createUrlTree(['/ingresar'], { queryParams: { returnUrl: state.url } });
};

/** Pantallas de visitante (login, registro): con sesión iniciada, lleva al perfil. */
export const guestGuard: CanActivateFn = () =>
  inject(AuthService).isAuthenticated() ? inject(Router).createUrlTree(['/perfil']) : true;
