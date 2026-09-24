import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

const DEFAULT_MESSAGES: Record<string, (error: unknown) => string> = {
  required: () => 'Este campo es obligatorio.',
  email: () => 'Ingresa un correo electrónico válido.',
  phone: () => 'Ingresa un teléfono válido (7 a 15 dígitos).',
  passwordStrength: () => 'Debe incluir al menos una letra y un número.',
  mismatch: () => 'Las contraseñas no coinciden.',
  minlength: (e) => `Mínimo ${(e as { requiredLength: number }).requiredLength} caracteres.`,
  maxlength: (e) => `Máximo ${(e as { requiredLength: number }).requiredLength} caracteres.`,
};

/** Muestra el primer error de un control, solo cuando el usuario ya lo tocó o se intentó enviar. */
@Component({
  selector: 'app-field-error',
  template: `
    @if (message(); as text) {
      <p class="field-error" [id]="id()" role="alert">{{ text }}</p>
    }
  `,
})
export class FieldErrorComponent {
  readonly control = input.required<AbstractControl | null>();
  /** id del <p>, para enlazarlo con aria-describedby del campo. */
  readonly id = input<string>();

  message(): string | null {
    const control = this.control();
    if (!control || !control.errors || !(control.touched || control.dirty)) {
      return null;
    }
    if (typeof control.errors['server'] === 'string') {
      return control.errors['server'];
    }
    const [key, value] = Object.entries(control.errors)[0];
    return DEFAULT_MESSAGES[key]?.(value) ?? 'El valor ingresado no es válido.';
  }
}
