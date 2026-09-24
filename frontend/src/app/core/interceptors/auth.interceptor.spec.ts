import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['token', 'logout']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/solicitudes' });
    router.navigate.and.resolveTo(true);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('agrega el Bearer token a las peticiones del solicitudes-service', () => {
    auth.token.and.returnValue('mi-token');

    http.get(`${environment.solicitudesApiUrl}/medicamentos`).subscribe();

    const req = controller.expectOne(`${environment.solicitudesApiUrl}/medicamentos`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer mi-token');
    req.flush([]);
  });

  it('NO envía el token al auth-service ni a terceros', () => {
    auth.token.and.returnValue('mi-token');

    http.post(`${environment.authApiUrl}/auth/login`, {}).subscribe();
    http.get('https://api.externa.com/datos').subscribe();

    expect(controller.expectOne(`${environment.authApiUrl}/auth/login`).request.headers.has('Authorization')).toBeFalse();
    expect(controller.expectOne('https://api.externa.com/datos').request.headers.has('Authorization')).toBeFalse();
  });

  it('ante un 401 del solicitudes-service cierra sesión y redirige al login', () => {
    auth.token.and.returnValue('token-vencido');
    let errorStatus = 0;

    http.get(`${environment.solicitudesApiUrl}/solicitudes`).subscribe({ error: (e) => (errorStatus = e.status) });
    controller
      .expectOne(`${environment.solicitudesApiUrl}/solicitudes`)
      .flush({ title: 'No autenticado' }, { status: 401, statusText: 'Unauthorized' });

    expect(errorStatus).toBe(401);
    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expired: 1, returnUrl: '/solicitudes' } });
  });

  it('un 401 del login (credenciales incorrectas) NO redirige: el formulario muestra el error', () => {
    http.post(`${environment.authApiUrl}/auth/login`, {}).subscribe({ error: () => undefined });
    controller
      .expectOne(`${environment.authApiUrl}/auth/login`)
      .flush({ title: 'Credenciales inválidas' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
