import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  finalize,
  firstValueFrom,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';
import { ApiClient } from '../http/api-client';
import {
  MessageResponse,
  RegisterRequest,
  TokenResponse,
  UpdateProfileRequest,
  UserProfile,
} from './auth.models';

/**
 * Sesión del usuario. El token de acceso vive SOLO en memoria (nunca en localStorage); el de
 * refresco es una cookie HttpOnly que el navegador envía a /api/v1/auth (ADR-002).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiClient);
  private readonly router = inject(Router);

  private readonly token = signal<string | null>(null);
  private refreshInFlight: Observable<string> | null = null;

  readonly user = signal<UserProfile | null>(null);
  readonly isAuthenticated = computed(() => this.token() !== null);

  accessToken(): string | null {
    return this.token();
  }

  register(request: RegisterRequest): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/register', request);
  }

  verifyEmail(token: string): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/verify', { token });
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/verify/resend', { email });
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/password/forgot', { email });
  }

  resetPassword(token: string, password: string): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/password/reset', { token, password });
  }

  login(email: string, password: string): Observable<UserProfile> {
    return this.api
      .post<TokenResponse>('/auth/login', { email, password }, { withCredentials: true })
      .pipe(
        tap((response) => this.token.set(response.accessToken)),
        switchMap(() => this.loadProfile()),
      );
  }

  /** Renueva el token de acceso; las llamadas concurrentes comparten una sola petición. */
  refresh(): Observable<string> {
    this.refreshInFlight ??= this.api
      .post<TokenResponse>('/auth/refresh', {}, { withCredentials: true })
      .pipe(
        map((response) => response.accessToken),
        tap((accessToken) => this.token.set(accessToken)),
        catchError((error: unknown) => {
          this.clear();
          return throwError(() => error);
        }),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    return this.refreshInFlight;
  }

  /** Al iniciar la app intenta recuperar la sesión desde la cookie, sin mostrar errores. */
  restoreSession(): Promise<void> {
    return firstValueFrom(
      this.refresh().pipe(
        switchMap(() => this.loadProfile()),
        map(() => undefined),
        catchError(() => of(undefined)),
      ),
    );
  }

  loadProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/me').pipe(tap((profile) => this.user.set(profile)));
  }

  updateProfile(changes: UpdateProfileRequest): Observable<UserProfile> {
    return this.api
      .patch<UserProfile>('/me', changes)
      .pipe(tap((profile) => this.user.set(profile)));
  }

  activateSellerProfile(storeName: string): Observable<SellerProfileResponse> {
    return this.api
      .post<SellerProfileResponse>('/me/seller-profile', {
        storeName,
        acceptSalesTerms: true,
      })
      .pipe(
        tap(() =>
          this.user.update((profile) => (profile ? { ...profile, seller: true } : profile)),
        ),
      );
  }

  /** Cambiar la contraseña cierra todas las sesiones: se vuelve a pedir el inicio de sesión. */
  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.api
      .post<void>('/me/password', { currentPassword, newPassword }, { withCredentials: true })
      .pipe(tap(() => this.clear()));
  }

  logout(): Observable<void> {
    return this.api.post<void>('/auth/logout', {}, { withCredentials: true }).pipe(
      catchError(() => of(undefined)),
      map(() => undefined),
      finalize(() => this.clear()),
    );
  }

  /** La sesión expiró: limpia el estado y lleva al inicio de sesión, recordando la página. */
  handleSessionExpired(): void {
    this.clear();
    const returnUrl = this.router.url;
    void this.router.navigate(['/ingresar'], {
      queryParams: returnUrl && returnUrl !== '/' ? { returnUrl } : {},
    });
  }

  clear(): void {
    this.token.set(null);
    this.user.set(null);
  }
}

export interface SellerProfileResponse {
  readonly storeName: string;
  readonly termsVersion: string;
  readonly termsAcceptedAt: string;
}
