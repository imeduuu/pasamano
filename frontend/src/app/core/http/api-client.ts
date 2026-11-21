import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';

export interface RequestOptions {
  params?: HttpParams | Record<string, string | number | boolean>;
  /** Envía cookies (necesario para el token de refresco HttpOnly). */
  withCredentials?: boolean;
}

/** Cliente HTTP de la API de Trato: antepone la URL base configurable a cada ruta. */
@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  url(path: string): string {
    const normalized = path.startsWith('/') ? path : `/${path}`;
    return `${this.config.apiBaseUrl}${normalized}`;
  }

  isApiUrl(url: string): boolean {
    return url.startsWith(this.config.apiBaseUrl);
  }

  get<T>(path: string, options: RequestOptions = {}): Observable<T> {
    return this.http.get<T>(this.url(path), options);
  }

  post<T>(path: string, body: unknown = {}, options: RequestOptions = {}): Observable<T> {
    return this.http.post<T>(this.url(path), body, options);
  }

  patch<T>(path: string, body: unknown, options: RequestOptions = {}): Observable<T> {
    return this.http.patch<T>(this.url(path), body, options);
  }

  delete<T>(path: string, options: RequestOptions = {}): Observable<T> {
    return this.http.delete<T>(this.url(path), options);
  }
}
