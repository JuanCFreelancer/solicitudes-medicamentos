export interface JwtPayload {
  sub?: string;
  email?: string;
  nombre?: string;
  /** Expiración en segundos (epoch). */
  exp?: number;
}

/**
 * Decodifica (sin verificar la firma) el payload de un JWT. Solo se usa para mostrar el
 * nombre del usuario y conocer la expiración; la validación real la hace el backend.
 */
export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((char) => '%' + char.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
