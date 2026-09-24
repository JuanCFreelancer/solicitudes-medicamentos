import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Medicamento, Solicitud, SolicitudRequest } from '../../../core/models/solicitud.models';
import { toApiError } from '../../../core/utils/api-error';
import { applyServerErrors } from '../../../core/utils/server-errors';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { emailFormat, notBlank, phoneFormat } from '../../../shared/validators/form-validators';
import { MedicamentoService } from '../services/medicamento.service';
import { SolicitudService } from '../services/solicitud.service';

type NoPosField = 'numeroOrden' | 'direccion' | 'telefono' | 'correoContacto';

/** Campos que solo aplican (y son obligatorios) cuando el medicamento es NO POS. */
const NO_POS_RULES: Record<NoPosField, ValidatorFn[]> = {
  numeroOrden: [notBlank, Validators.maxLength(50)],
  direccion: [notBlank, Validators.maxLength(255)],
  telefono: [notBlank, phoneFormat],
  correoContacto: [notBlank, emailFormat, Validators.maxLength(254)],
};
const NO_POS_FIELDS = Object.keys(NO_POS_RULES) as NoPosField[];

@Component({
  selector: 'app-solicitud-form',
  imports: [ReactiveFormsModule, RouterLink, AlertComponent, FieldErrorComponent],
  templateUrl: './solicitud-form.component.html',
})
export class SolicitudFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly medicamentoService = inject(MedicamentoService);
  private readonly solicitudService = inject(SolicitudService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly medicamentos = signal<Medicamento[]>([]);
  protected readonly loadingCatalog = signal(true);
  protected readonly catalogError = signal<string | null>(null);

  protected readonly selected = signal<Medicamento | null>(null);
  /** Controla si se muestra (y se exige) el bloque de datos adicionales. */
  protected readonly isNoPos = computed(() => this.selected()?.esPos === false);

  protected readonly submitting = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly created = signal<Solicitud | null>(null);

  protected readonly form = this.fb.group({
    medicamentoId: this.fb.control<number | null>(null, Validators.required),
    numeroOrden: [''],
    direccion: [''],
    telefono: [''],
    correoContacto: [''],
  });

  constructor() {
    this.loadCatalog();
    this.form.controls.medicamentoId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((id) => this.onMedicamentoChange(id));
  }

  protected loadCatalog(): void {
    this.loadingCatalog.set(true);
    this.catalogError.set(null);
    this.medicamentoService
      .list()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (medicamentos) => {
          this.medicamentos.set(medicamentos);
          this.loadingCatalog.set(false);
        },
        error: (error: unknown) => {
          this.catalogError.set(toApiError(error).message);
          this.loadingCatalog.set(false);
        },
      });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.host.nativeElement.querySelector<HTMLElement>('input.ng-invalid, select.ng-invalid')?.focus();
      return;
    }

    this.submitting.set(true);
    this.serverError.set(null);
    this.created.set(null);

    this.solicitudService.create(this.buildRequest()).subscribe({
      next: (created) => {
        this.form.reset();
        this.created.set(created);
        this.submitting.set(false);
      },
      error: (error: unknown) => {
        const apiError = toApiError(error);
        const shownOnFields = applyServerErrors(this.form, apiError.fieldErrors);
        this.serverError.set(shownOnFields ? 'Revisa los campos marcados en el formulario.' : apiError.message);
        this.submitting.set(false);
      },
    });
  }

  /** Para un medicamento POS solo se envía el id: los datos adicionales no aplican. */
  private buildRequest(): SolicitudRequest {
    const value = this.form.getRawValue();
    const request: SolicitudRequest = { medicamentoId: value.medicamentoId as number };
    if (!this.isNoPos()) {
      return request;
    }
    return {
      ...request,
      numeroOrden: value.numeroOrden.trim(),
      direccion: value.direccion.trim(),
      telefono: value.telefono.trim(),
      correoContacto: value.correoContacto.trim(),
    };
  }

  private onMedicamentoChange(id: number | null): void {
    const medicamento = this.medicamentos().find((item) => item.id === id) ?? null;
    this.selected.set(medicamento);
    if (medicamento) {
      this.created.set(null);
    }
    this.configureNoPosFields(medicamento?.esPos === false);
  }

  /**
   * NO POS: activa las validaciones obligatorias de los cuatro campos.
   * POS (o sin selección): las quita y limpia lo que se hubiera escrito.
   */
  private configureNoPosFields(required: boolean): void {
    for (const name of NO_POS_FIELDS) {
      const control = this.form.controls[name];
      if (required) {
        control.setValidators(NO_POS_RULES[name]);
      } else {
        control.clearValidators();
        control.reset('');
      }
      control.updateValueAndValidity({ emitEvent: false });
    }
  }
}
