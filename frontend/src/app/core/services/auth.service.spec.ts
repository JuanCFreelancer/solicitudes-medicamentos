import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/** Construye un JWT sin firma válida (el frontend solo lee el payload). */
function fakeJwt(payload: object): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.firma`;
}

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('login guarda la sesión con los datos del token y marca al usuario como autenticado', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const token = fakeJwt({ sub: '1', nombre: 'Ana', email: 'ana@correo.com', exp });

    service.login({ email: 'ana@correo.com', password: 'Clave1234' }).subscribe();
    const req = http.expectOne(`${environment.authApiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({ token, tipo: 'Bearer', expiraEn: 3600 });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.session()?.nombre).toBe('Ana');
    expect(service.token()).toBe(token);
    expect(sessionStorage.getItem('session')).toContain('ana@correo.com');
  });

  it('logout limpia la sesión y el almacenamiento', () => {
    service.login({ email: 'a@b.co', password: 'x' }).subscribe();
    http.expectOne(`${environment.authApiUrl}/auth/login`).flush({
      token: fakeJwt({ exp: Math.floor(Date.now() / 1000) + 60 }),
      tipo: 'Bearer',
      expiraEn: 60,
    });

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.token()).toBeNull();
    expect(sessionStorage.getItem('session')).toBeNull();
  });

  it('un token vencido no cuenta como sesión activa', () => {
    service.login({ email: 'a@b.co', password: 'x' }).subscribe();
    http.expectOne(`${environment.authApiUrl}/auth/login`).flush({
      token: fakeJwt({ exp: Math.floor(Date.now() / 1000) - 10 }),
      tipo: 'Bearer',
      expiraEn: 60,
    });

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.session()).toBeNull();
  });

  it('register envía los datos al endpoint /auth/register', () => {
    service.register({ nombre: 'Ana', email: 'ana@correo.com', password: 'Clave1234' }).subscribe();

    const req = http.expectOne(`${environment.authApiUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nombre: 'Ana', email: 'ana@correo.com', password: 'Clave1234' });
    req.flush({ id: 1, nombre: 'Ana', email: 'ana@correo.com' });
  });
});
