export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
}

export interface TokenResponse {
  token: string;
  tipo: string;
  /** Segundos de vigencia del token. */
  expiraEn: number;
}

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
}

/** Sesión activa en el navegador. */
export interface Session {
  token: string;
  nombre: string;
  email: string;
  /** Instante de expiración en milisegundos (epoch). */
  expiresAt: number;
}
