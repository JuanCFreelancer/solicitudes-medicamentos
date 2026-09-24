import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="app-header">
      <div class="container app-header-inner">
        <a class="brand" routerLink="/">
          <img class="brand-logo" src="logo-nueva-eps.png" alt="Nueva EPS" height="40" />
          <span class="brand-divider"></span>
          <span class="brand-text">Solicitudes de medicamentos</span>
        </a>

        @if (auth.session(); as session) {
          <nav class="app-nav" aria-label="Navegación principal">
            <a routerLink="/solicitudes" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Mis solicitudes</a>
            <a routerLink="/solicitudes/nueva" routerLinkActive="active">Nueva solicitud</a>
          </nav>
          <div class="app-user">
            <span class="app-user-name" [title]="session.email">{{ session.nombre || session.email }}</span>
            <button type="button" class="btn-logout" (click)="logout()">Cerrar sesión</button>
          </div>
        }
      </div>
    </header>
  `,
})
export class HeaderComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
