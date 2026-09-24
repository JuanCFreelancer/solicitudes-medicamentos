import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, RegisterRequest, Session, TokenResponse, Usuario } from '../models/auth.models';
import { decodeJwtPayload } from '../utils/jwt';

const STORAGE_KEY = 'session';

/**
 * Único punto que conoce el estado de sesión y habla con el auth-service.
 * La sesión se guarda en sessionStorage: se pierde al cerrar la pestaña.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.authApiUrl}/auth`;

  private readonly sessionState = signal<Session | null>(this.restoreSession());
  /** Sesión actual (null si no hay). Es un signal: la UI reacciona a login/logout. */
  readonly session = this.sessionState.asReadonly();

  register(request: RegisterRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.baseUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${this.baseUrl}/login`, request)
      .pipe(tap((response) => this.startSession(response)));
  }

  logout(): void {
    this.sessionState.set(null);
    this.storageRemove();
  }

  /** true si hay un token y no ha expirado; si expiró, limpia la sesión. */
  isAuthenticated(): boolean {
    const session = this.sessionState();
    if (!session) {
      return false;
    }
    if (session.expiresAt <= Date.now()) {
      this.logout();
      return false;
    }
    return true;
  }

  token(): string | null {
    return this.isAuthenticated() ? this.sessionState()!.token : null;
  }

  private startSession(response: TokenResponse): void {
    const payload = decodeJwtPayload(response.token);
    const session: Session = {
      token: response.token,
      nombre: payload?.nombre ?? '',
      email: payload?.email ?? '',
      expiresAt: payload?.exp ? payload.exp * 1000 : Date.now() + response.expiraEn * 1000,
    };
    this.sessionState.set(session);
    this.storageWrite(session);
  }

  private restoreSession(): Session | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      const session = raw ? (JSON.parse(raw) as Session) : null;
      return session && session.expiresAt > Date.now() ? session : null;
    } catch {
      return null;
    }
  }

  private storageWrite(session: Session): void {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      /* almacenamiento no disponible: la sesión vive solo en memoria */
    }
  }

  private storageRemove(): void {
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      /* nada que limpiar */
    }
  }
}
