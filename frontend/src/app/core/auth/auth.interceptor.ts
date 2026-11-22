import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { ApiClient } from '../http/api-client';
import { AuthService } from './auth.service';

/** Endpoints de /auth que no llevan token de acceso (solo /auth/logout lo requiere). */
export function isPublicAuthUrl(url: string): boolean {
  return url.includes('/auth/') && !url.includes('/auth/logout');
}

function withBearer(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

/**
 * Agrega el token de acceso a las llamadas a la API y, si responde 401, renueva el token de forma
 * transparente y reintenta una sola vez (CU-02 5a). Si la renovación falla, vuelve al login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const api = inject(ApiClient);
  const auth = inject(AuthService);

  if (!api.isApiUrl(request.url) || isPublicAuthUrl(request.url)) {
    return next(request);
  }

  return next(withBearer(request, auth.accessToken())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError(() => {
          auth.handleSessionExpired();
          return throwError(() => error);
        }),
        switchMap((token) => next(withBearer(request, token))),
      );
    }),
  );
};
