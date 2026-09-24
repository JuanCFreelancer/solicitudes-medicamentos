import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Medicamento, Solicitud } from '../../../core/models/solicitud.models';
import { MedicamentoService } from '../services/medicamento.service';
import { SolicitudService } from '../services/solicitud.service';
import { SolicitudFormComponent } from './solicitud-form.component';

const ACETAMINOFEN: Medicamento = { id: 1, nombre: 'Acetaminofén 500 mg', esPos: true };
const ADALIMUMAB: Medicamento = { id: 2, nombre: 'Adalimumab 40 mg', esPos: false };
const SEMAGLUTIDA: Medicamento = { id: 3, nombre: 'Semaglutida 1 mg', esPos: false };

const CREADA: Solicitud = {
  id: 77,
  medicamento: ACETAMINOFEN,
  numeroOrden: null,
  direccion: null,
  telefono: null,
  correoContacto: null,
  createdAt: '2026-01-01T10:00:00Z',
};

describe('SolicitudFormComponent (formulario condicional NO POS)', () => {
  let fixture: ComponentFixture<SolicitudFormComponent>;
  let solicitudService: jasmine.SpyObj<SolicitudService>;

  const root = () => fixture.nativeElement as HTMLElement;
  const field = (id: string) => root().querySelector<HTMLInputElement>(`#${id}`);
  const noPosSection = () => root().querySelector('fieldset.fieldset-no-pos');
  const errors = () => Array.from(root().querySelectorAll('.field-error')).map((e) => e.textContent!.trim());

  /** Selecciona por posición (0 = placeholder) como lo haría el usuario en el <select>. */
  function selectMedicamento(index: number): void {
    const select = field('medicamentoId') as unknown as HTMLSelectElement;
    select.selectedIndex = index;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function type(id: string, value: string): void {
    const input = field(id)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submit(): void {
    root().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  beforeEach(() => {
    solicitudService = jasmine.createSpyObj<SolicitudService>('SolicitudService', ['create', 'list']);
    const medicamentoService = jasmine.createSpyObj<MedicamentoService>('MedicamentoService', ['list']);
    medicamentoService.list.and.returnValue(of([ACETAMINOFEN, ADALIMUMAB, SEMAGLUTIDA]));

    TestBed.configureTestingModule({
      imports: [SolicitudFormComponent],
      providers: [
        provideRouter([]),
        { provide: SolicitudService, useValue: solicitudService },
        { provide: MedicamentoService, useValue: medicamentoService },
      ],
    });
    fixture = TestBed.createComponent(SolicitudFormComponent);
    fixture.detectChanges();
  });

  it('carga el catálogo y marca los NO POS en el listado', () => {
    const options = Array.from(root().querySelectorAll('option')).map((o) => o.textContent!.trim());
    expect(options).toContain('Acetaminofén 500 mg');
    expect(options).toContain('Adalimumab 40 mg (NO POS)');
  });

  it('con un medicamento POS NO muestra los datos adicionales', () => {
    selectMedicamento(1);
    expect(noPosSection()).toBeNull();
  });

  it('con un medicamento NO POS muestra los cuatro campos adicionales', () => {
    selectMedicamento(2);

    expect(noPosSection()).not.toBeNull();
    for (const id of ['numeroOrden', 'direccion', 'telefono', 'correoContacto']) {
      expect(field(id)).withContext(id).not.toBeNull();
    }
  });

  it('NO POS: no envía si faltan los campos y muestra cada error', () => {
    selectMedicamento(2);

    submit();

    expect(solicitudService.create).not.toHaveBeenCalled();
    expect(errors().length).toBe(4);
    expect(errors().every((message) => message === 'Este campo es obligatorio.')).toBeTrue();
  });

  it('NO POS: valida formato de teléfono y correo', () => {
    selectMedicamento(2);
    type('numeroOrden', 'ORD-1');
    type('direccion', 'Calle 1 # 2-3');
    type('telefono', 'abc');
    type('correoContacto', 'correo-malo');

    submit();

    expect(solicitudService.create).not.toHaveBeenCalled();
    expect(errors()).toContain('Ingresa un teléfono válido (7 a 15 dígitos).');
    expect(errors()).toContain('Ingresa un correo electrónico válido.');
  });

  it('NO POS: un campo con solo espacios cuenta como vacío', () => {
    selectMedicamento(2);
    type('numeroOrden', '    ');
    type('direccion', 'Calle 1');
    type('telefono', '3001234567');
    type('correoContacto', 'a@b.co');

    submit();

    expect(solicitudService.create).not.toHaveBeenCalled();
    expect(errors()).toEqual(['Este campo es obligatorio.']);
  });

  it('NO POS completo: envía los cuatro campos limpios de espacios', () => {
    solicitudService.create.and.returnValue(of({ ...CREADA, id: 78, medicamento: ADALIMUMAB }));
    selectMedicamento(2);
    type('numeroOrden', ' ORD-9 ');
    type('direccion', ' Calle 9 # 8-7 ');
    type('telefono', '300 123 4567');
    type('correoContacto', ' ana@correo.com ');

    submit();

    expect(solicitudService.create).toHaveBeenCalledOnceWith({
      medicamentoId: 2,
      numeroOrden: 'ORD-9',
      direccion: 'Calle 9 # 8-7',
      telefono: '300 123 4567',
      correoContacto: 'ana@correo.com',
    });
    expect(root().querySelector('.alert-success')?.textContent).toContain('Solicitud #78');
  });

  it('POS: envía únicamente el medicamento, sin campos adicionales', () => {
    solicitudService.create.and.returnValue(of(CREADA));
    selectMedicamento(1);

    submit();

    expect(solicitudService.create).toHaveBeenCalledOnceWith({ medicamentoId: 1 });
    expect(root().querySelector('.alert-success')?.textContent).toContain('Solicitud #77');
  });

  it('sin medicamento seleccionado no envía y avisa que es obligatorio', () => {
    submit();

    expect(solicitudService.create).not.toHaveBeenCalled();
    expect(errors()).toEqual(['Este campo es obligatorio.']);
  });

  it('al cambiar de NO POS a POS oculta la sección y descarta lo escrito', () => {
    selectMedicamento(2);
    type('numeroOrden', 'ORD-1');
    solicitudService.create.and.returnValue(of(CREADA));

    selectMedicamento(1);
    expect(noPosSection()).toBeNull();
    submit();

    // los datos escritos para el NO POS no viajan con el POS
    expect(solicitudService.create).toHaveBeenCalledOnceWith({ medicamentoId: 1 });

    // y si vuelve a un NO POS, los campos aparecen vacíos
    selectMedicamento(2);
    expect(field('numeroOrden')!.value).toBe('');
  });

  it('al cambiar entre dos NO POS conserva los datos ya escritos', () => {
    selectMedicamento(2);
    type('direccion', 'Calle 1');

    selectMedicamento(3);

    expect(field('direccion')!.value).toBe('Calle 1');
  });

  it('muestra junto al campo los errores de validación devueltos por el backend (400)', () => {
    solicitudService.create.and.returnValue(
      throwError(() => new HttpErrorResponse({
        status: 400,
        error: { title: 'Error de validación', errors: { telefono: 'El teléfono no es válido para la EPS' } },
      })),
    );
    selectMedicamento(2);
    type('numeroOrden', 'ORD-1');
    type('direccion', 'Calle 1');
    type('telefono', '3001234567');
    type('correoContacto', 'a@b.co');

    submit();

    expect(errors()).toContain('El teléfono no es válido para la EPS');
    expect(root().querySelector('.alert-error')?.textContent).toContain('Revisa los campos marcados');
  });

  it('muestra un mensaje general si el backend rechaza el medicamento (422)', () => {
    solicitudService.create.and.returnValue(
      throwError(() => new HttpErrorResponse({
        status: 422,
        error: { title: 'Medicamento no disponible', detail: 'El medicamento con id 1 no existe o no está disponible' },
      })),
    );
    selectMedicamento(1);

    submit();

    expect(root().querySelector('.alert-error')?.textContent).toContain('no existe o no está disponible');
  });
});
