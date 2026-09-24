import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './layout/header.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent],
  template: `
    <app-header />
    <main class="container fade-in">
      <router-outlet />
    </main>
    <footer class="app-footer">
      <div>Prueba técnica · <strong>Nueva EPS</strong> · Solicitudes de medicamentos</div>
      <div class="app-footer-links">
        <a href="https://nuevaeps.com.co" target="_blank" rel="noopener">nuevaeps.com.co</a>
      </div>
    </footer>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    main {
      flex: 1;
    }
  `,
})
export class AppComponent {}
