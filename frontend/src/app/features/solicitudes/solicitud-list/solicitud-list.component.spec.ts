import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Page, Solicitud } from '../../../core/models/solicitud.models';
import { SolicitudService } from '../services/solicitud.service';
import { SolicitudListComponent } from './solicitud-list.component';

const solicitud = (id: number, esPos: boolean): Solicitud => ({
  id,
  medicamento: { id: esPos ? 1 : 2, nombre: esPos ? 'Acetaminofén' : 'Adalimumab', esPos },
  numeroOrden: esPos ? null : `ORD-${id}`,
  direccion: esPos ? null : 'Calle 1',
  telefono: esPos ? null : '3001234567',
  correoContacto: esPos ? null : 'a@b.co',
  createdAt: '2026-01-01T10:00:00Z',
});

const page = (content: Solicitud[], pageNumber: number, size: number, total: number): Page<Solicitud> => ({
  content,
  page: pageNumber,
  size,
  totalElements: total,
  totalPages: Math.ceil(total / size),
});

describe('SolicitudListComponent', () => {
  let fixture: ComponentFixture<SolicitudListComponent>;
  let service: jasmine.SpyObj<SolicitudService>;
  const root = () => fixture.nativeElement as HTMLElement;

  async function create(): Promise<void> {
    fixture = TestBed.createComponent(SolicitudListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    service = jasmine.createSpyObj<SolicitudService>('SolicitudService', ['list', 'create']);
    TestBed.configureTestingModule({
      imports: [SolicitudListComponent],
      providers: [provideRouter([]), { provide: SolicitudService, useValue: service }],
    });
  });

  it('pide la primera página (0) con 10 elementos y pinta las filas', async () => {
    service.list.and.returnValue(of(page([solicitud(2, false), solicitud(1, true)], 0, 10, 2)));

    await create();

    expect(service.list).toHaveBeenCalledWith(0, 10);
    const rows = root().querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Adalimumab');
    expect(rows[0].textContent).toContain('NO POS');
    expect(rows[0].textContent).toContain('ORD-2');
    expect(rows[1].textContent).toContain('POS');
    expect(rows[1].textContent).toContain('—'); // datos NO POS ausentes en un medicamento POS
  });

  it('al pulsar Siguiente pide la página 1', async () => {
    service.list.and.callFake((p, s) => of(page([solicitud(p + 1, true)], p, s, 25)));
    await create();

    const next = Array.from(root().querySelectorAll('button')).find((b) => b.textContent!.trim() === 'Siguiente')!;
    next.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(service.list).toHaveBeenCalledWith(1, 10);
  });

  it('al cambiar el tamaño vuelve a la página 0', async () => {
    service.list.and.callFake((p, s) => of(page([solicitud(1, true)], p, s, 60)));
    await create();
    Array.from(root().querySelectorAll('button')).find((b) => b.textContent!.trim() === 'Siguiente')!.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const select = root().querySelector('select')!;
    select.value = '20';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(service.list).toHaveBeenCalledWith(0, 20);
  });

  it('sin solicitudes muestra el estado vacío con enlace para crear la primera', async () => {
    service.list.and.returnValue(of(page([], 0, 10, 0)));

    await create();

    expect(root().querySelector('.empty-state')?.textContent).toContain('Aún no has creado ninguna solicitud');
    expect(root().querySelector('table')).toBeNull();
  });

  it('si la API falla muestra el error y permite reintentar', async () => {
    service.list.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    await create();

    expect(root().querySelector('.alert-error')?.textContent).toContain('No se pudo conectar con el servidor');

    service.list.and.returnValue(of(page([solicitud(1, true)], 0, 10, 1)));
    root().querySelector<HTMLButtonElement>('.alert-error button')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(root().querySelector('.alert-error')).toBeNull();
    expect(root().querySelectorAll('tbody tr').length).toBe(1);
  });
});
