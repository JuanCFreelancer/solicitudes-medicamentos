import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('guards', () => {
  let auth: jasmine.SpyObj<AuthService>;
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/solicitudes/nueva' } as RouterStateSnapshot;

  const run = (guard: typeof authGuard) => TestBed.runInInjectionContext(() => guard(route, state));

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  it('authGuard deja pasar a un usuario autenticado', () => {
    auth.isAuthenticated.and.returnValue(true);
    expect(run(authGuard)).toBeTrue();
  });

  it('authGuard redirige al login recordando la ruta solicitada', () => {
    auth.isAuthenticated.and.returnValue(false);

    const result = run(authGuard) as UrlTree;

    expect(result.toString()).toBe('/login?returnUrl=%2Fsolicitudes%2Fnueva');
  });

  it('guestGuard manda a las solicitudes a quien ya inició sesión', () => {
    auth.isAuthenticated.and.returnValue(true);

    expect((run(guestGuard) as UrlTree).toString()).toBe('/solicitudes');
  });

  it('guestGuard permite ver login/registro sin sesión', () => {
    auth.isAuthenticated.and.returnValue(false);
    expect(run(guestGuard)).toBeTrue();
  });
});
