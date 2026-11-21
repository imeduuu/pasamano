import { InjectionToken } from '@angular/core';

/** Configuración leída en tiempo de ejecución desde /app-config.json (12-factor). */
export interface AppConfig {
  /** URL base de la API, por ejemplo "/api/v1" o "https://api.ejemplo.cl/api/v1". */
  readonly apiBaseUrl: string;
}

export const DEFAULT_APP_CONFIG: AppConfig = { apiBaseUrl: '/api/v1' };

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG', {
  providedIn: 'root',
  factory: () => DEFAULT_APP_CONFIG,
});

/** Carga la configuración; si falla, usa los valores por defecto para no bloquear el arranque. */
export async function loadAppConfig(
  fetchFn: typeof fetch = fetch,
  url = '/app-config.json',
): Promise<AppConfig> {
  try {
    const response = await fetchFn(url, { cache: 'no-store' });
    if (!response.ok) {
      return DEFAULT_APP_CONFIG;
    }
    const raw = (await response.json()) as Partial<AppConfig>;
    const apiBaseUrl =
      typeof raw.apiBaseUrl === 'string' && raw.apiBaseUrl.trim() !== ''
        ? raw.apiBaseUrl.replace(/\/+$/, '')
        : DEFAULT_APP_CONFIG.apiBaseUrl;
    return { apiBaseUrl };
  } catch {
    return DEFAULT_APP_CONFIG;
  }
}
