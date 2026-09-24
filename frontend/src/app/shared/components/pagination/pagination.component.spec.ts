import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginationComponent } from './pagination.component';

describe('PaginationComponent', () => {
  let fixture: ComponentFixture<PaginationComponent>;
  const el = () => fixture.nativeElement as HTMLElement;
  const buttons = () => Array.from(el().querySelectorAll<HTMLButtonElement>('button'));
  const button = (text: string) => buttons().find((b) => b.textContent?.trim() === text)!;

  function setup(page: number, size: number, totalElements: number, totalPages: number) {
    fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('size', size);
    fixture.componentRef.setInput('totalElements', totalElements);
    fixture.componentRef.setInput('totalPages', totalPages);
    fixture.detectChanges();
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [PaginationComponent] }));

  it('muestra el rango visible y el total (página 2 de 25 elementos, 10 por página)', () => {
    setup(1, 10, 25, 3);
    expect(el().querySelector('.pagination-summary')?.textContent).toContain('11–20');
    expect(el().querySelector('.pagination-summary')?.textContent).toContain('25');
  });

  it('la última página recorta el rango al total', () => {
    setup(2, 10, 25, 3);
    expect(el().querySelector('.pagination-summary')?.textContent).toContain('21–25');
  });

  it('deshabilita Anterior en la primera página y Siguiente en la última', () => {
    setup(0, 10, 25, 3);
    expect(button('Anterior').disabled).toBeTrue();
    expect(button('Siguiente').disabled).toBeFalse();

    setup(2, 10, 25, 3);
    expect(button('Anterior').disabled).toBeFalse();
    expect(button('Siguiente').disabled).toBeTrue();
  });

  it('emite pageChange (base 0) al pulsar Siguiente y al elegir un número', () => {
    setup(0, 10, 25, 3);
    const emitted: number[] = [];
    fixture.componentInstance.pageChange.subscribe((p) => emitted.push(p));

    button('Siguiente').click();
    button('3').click();
    button('1').click(); // ya es la página actual: no debe emitir

    expect(emitted).toEqual([1, 2]);
  });

  it('emite sizeChange al cambiar el tamaño de página', () => {
    setup(0, 10, 25, 3);
    let size = 0;
    fixture.componentInstance.sizeChange.subscribe((s) => (size = s));

    const select = el().querySelector('select')!;
    select.value = '20';
    select.dispatchEvent(new Event('change'));

    expect(size).toBe(20);
  });

  it('limita los números visibles a 5 con muchas páginas', () => {
    setup(10, 10, 300, 30);
    const numbered = buttons().filter((b) => /^\d+$/.test(b.textContent!.trim()));
    expect(numbered.map((b) => b.textContent!.trim())).toEqual(['9', '10', '11', '12', '13']);
  });

  it('sin resultados muestra 0–0 de 0 y no rompe', () => {
    setup(0, 10, 0, 0);
    expect(el().querySelector('.pagination-summary')?.textContent).toContain('0–0');
  });
});
