import { Component, computed, input, output } from '@angular/core';

const MAX_VISIBLE_PAGES = 5;

/**
 * Paginador presentacional: no sabe de dónde vienen los datos, solo emite los cambios.
 * {@code page} es base 0 (igual que la API); en pantalla se muestra base 1.
 */
@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly sizeOptions = input<number[]>([5, 10, 20, 50]);

  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();

  protected readonly from = computed(() => (this.totalElements() === 0 ? 0 : this.page() * this.size() + 1));
  protected readonly to = computed(() => Math.min((this.page() + 1) * this.size(), this.totalElements()));
  protected readonly isFirst = computed(() => this.page() <= 0);
  protected readonly isLast = computed(() => this.page() >= this.totalPages() - 1);

  /** Ventana de hasta 5 números de página centrada en la actual. */
  protected readonly pages = computed(() => {
    const total = this.totalPages();
    const visible = Math.min(MAX_VISIBLE_PAGES, total);
    let start = Math.max(0, this.page() - Math.floor(visible / 2));
    start = Math.min(start, total - visible);
    return Array.from({ length: visible }, (_, index) => start + index);
  });

  protected goTo(page: number): void {
    if (page >= 0 && page < this.totalPages() && page !== this.page()) {
      this.pageChange.emit(page);
    }
  }

  protected changeSize(event: Event): void {
    this.sizeChange.emit(Number((event.target as HTMLSelectElement).value));
  }
}
