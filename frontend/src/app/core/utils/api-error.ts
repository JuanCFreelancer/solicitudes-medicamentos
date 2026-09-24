import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo de error de las APIs (RFC 7807 + mapa de errores por campo). */
interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}

export interface ApiError {
  /** Mensaje legible para mostrar al usuario. */
  message: string;
  /** Mensajes por nombre de campo (solo en errores de validación). */
  fieldErrors: Record<string, string>;
}

const NETWORK_ERROR = 'No se pudo conectar con el servidor. Verifica tu conexión e intenta de nuevo.';
const UNEXPECTED_ERROR = 'Ocurrió un error inesperado. Intenta de nuevo más tarde.';

/** Convierte cualquier error HTTP en un {@link ApiError} uniforme para la UI. */
export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { message: UNEXPECTED_ERROR, fieldErrors: {} };
  }
  if (error.status === 0) {
    return { message: NETWORK_ERROR, fieldErrors: {} };
  }
  const problem = error.error as ProblemDetail | null;
  if (problem && typeof problem === 'object') {
    return {
      message: problem.detail ?? problem.title ?? UNEXPECTED_ERROR,
      fieldErrors: problem.errors ?? {},
    };
  }
  return { message: UNEXPECTED_ERROR, fieldErrors: {} };
}
