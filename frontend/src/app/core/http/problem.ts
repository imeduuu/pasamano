import { HttpErrorResponse } from '@angular/common/http';

/** Error de la API en formato Problem Details (RFC 9457) con un `code` estable. */
export interface ApiProblem {
  readonly status: number;
  readonly code: string;
  readonly detail: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export const NETWORK_ERROR =
  'No pudimos conectar con el servidor. Revisa tu conexión e intenta nuevamente.';
export const UNEXPECTED_ERROR = 'Ocurrió un error inesperado. Intenta nuevamente más tarde.';

interface RawProblem {
  code?: unknown;
  detail?: unknown;
  errors?: unknown;
}

/** Convierte cualquier error HTTP en un problema con mensajes en español para mostrar. */
export function toProblem(error: unknown): ApiProblem {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, code: 'UNKNOWN', detail: UNEXPECTED_ERROR, fieldErrors: {} };
  }
  if (error.status === 0) {
    return { status: 0, code: 'NETWORK_ERROR', detail: NETWORK_ERROR, fieldErrors: {} };
  }
  const body: RawProblem =
    error.error !== null && typeof error.error === 'object' ? (error.error as RawProblem) : {};
  const fieldErrors: Record<string, string> = {};
  if (Array.isArray(body.errors)) {
    for (const item of body.errors as { field?: unknown; message?: unknown }[]) {
      if (typeof item.field === 'string' && typeof item.message === 'string') {
        fieldErrors[item.field] ??= item.message;
      }
    }
  }
  return {
    status: error.status,
    code: typeof body.code === 'string' ? body.code : `HTTP_${error.status}`,
    detail: typeof body.detail === 'string' && body.detail ? body.detail : UNEXPECTED_ERROR,
    fieldErrors,
  };
}
