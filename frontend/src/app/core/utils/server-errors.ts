import { AbstractControl, FormGroup } from '@angular/forms';

/**
 * Marca en el formulario los errores de validación devueltos por el backend
 * (clave = nombre del control). Devuelve true si al menos un error se pudo asociar a un campo.
 */
export function applyServerErrors(form: FormGroup, fieldErrors: Record<string, string>): boolean {
  let applied = false;
  for (const [field, message] of Object.entries(fieldErrors)) {
    const control: AbstractControl | null = form.get(field);
    if (control) {
      control.setErrors({ ...control.errors, server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
