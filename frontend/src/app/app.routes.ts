import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

/** Cada feature se carga bajo demanda (lazy loading): el login no descarga el código de solicitudes. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'solicitudes' },
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Iniciar sesión',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    canActivate: [guestGuard],
    title: 'Crear cuenta',
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'solicitudes',
    canActivate: [authGuard],
    loadChildren: () => import('./features/solicitudes/solicitudes.routes').then((m) => m.SOLICITUDES_ROUTES),
  },
  { path: '**', redirectTo: 'solicitudes' },
];
