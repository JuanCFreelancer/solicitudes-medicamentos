import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * - Adjunta el Bearer token solo a las peticiones dirigidas al solicitudes-service
 *   (el token nunca se envía a terceros).
 * - Si esa API responde 401 (token vencido o inválido), cierra la sesión y lleva al login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isProtectedApi = request.url.startsWith(environment.solicitudesApiUrl);
  const token = isProtectedApi ? auth.token() : null;
  const authorized = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (isProtectedApi && error instanceof HttpErrorResponse && error.status === 401) {
        auth.logout();
        void router.navigate(['/login'], {
          queryParams: { expired: 1, returnUrl: router.url },
        });
      }
      return throwError(() => error);
    }),
  );
};
