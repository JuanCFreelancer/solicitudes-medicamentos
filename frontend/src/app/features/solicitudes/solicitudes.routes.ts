import { Routes } from '@angular/router';

export const SOLICITUDES_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis solicitudes',
    loadComponent: () => import('./solicitud-list/solicitud-list.component').then((m) => m.SolicitudListComponent),
  },
  {
    path: 'nueva',
    title: 'Nueva solicitud',
    loadComponent: () => import('./solicitud-form/solicitud-form.component').then((m) => m.SolicitudFormComponent),
  },
];
