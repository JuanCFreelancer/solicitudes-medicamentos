import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { Page, Solicitud } from '../../../core/models/solicitud.models';
import { toApiError } from '../../../core/utils/api-error';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { SolicitudService } from '../services/solicitud.service';

const DEFAULT_PAGE_SIZE = 10;

@Component({
  selector: 'app-solicitud-list',
  imports: [DatePipe, RouterLink, AlertComponent, PaginationComponent],
  templateUrl: './solicitud-list.component.html',
})
export class SolicitudListComponent {
  private readonly solicitudService = inject(SolicitudService);

  protected readonly page = signal(0);
  protected readonly size = signal(DEFAULT_PAGE_SIZE);
  /** Incrementarlo fuerza una nueva consulta con la misma página (botón "Reintentar"). */
  private readonly reloadTick = signal(0);

  protected readonly data = signal<Page<Solicitud> | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly isEmpty = computed(() => this.data()?.totalElements === 0);

  private readonly query = computed(() => ({ page: this.page(), size: this.size(), tick: this.reloadTick() }));

  constructor() {
    // switchMap cancela la petición anterior si el usuario cambia de página rápidamente.
    toObservable(this.query)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set(null);
        }),
        switchMap(({ page, size }) =>
          this.solicitudService.list(page, size).pipe(
            catchError((error: unknown) => {
              this.errorMessage.set(toApiError(error).message);
              this.loading.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((data) => {
        this.data.set(data);
        this.loading.set(false);
      });
  }

  protected goToPage(page: number): void {
    this.page.set(page);
  }

  protected changeSize(size: number): void {
    this.size.set(size);
    this.page.set(0);
  }

  protected retry(): void {
    this.reloadTick.update((tick) => tick + 1);
  }
}
