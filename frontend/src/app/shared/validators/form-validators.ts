import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const PHONE_PATTERN = /^\+?\d{7,15}$/;
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).+$/;

/** Reglas alineadas con las del backend para que el usuario vea el error antes de enviar. */

/** Como Validators.required, pero también rechaza textos de solo espacios. */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  return typeof value === 'string' && value.trim().length > 0 ? null : { required: true };
};

export const emailFormat: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '').trim();
  return value === '' || EMAIL_PATTERN.test(value) ? null : { email: true };
};

/** Acepta separadores habituales ("300 123-4567", "(300) 1234567"); exige 7–15 dígitos, "+" inicial opcional. */
export const phoneFormat: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '').trim();
  if (value === '') {
    return null;
  }
  return PHONE_PATTERN.test(value.replace(/[\s()-]/g, '')) ? null : { phone: true };
};

/** Al menos una letra y un número. */
export const passwordStrength: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');
  return value === '' || PASSWORD_PATTERN.test(value) ? null : { passwordStrength: true };
};

/** Validador de grupo: los dos campos deben coincidir. El error se marca en el segundo campo. */
export function fieldsMatch(field: string, confirmField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const confirm = group.get(confirmField);
    if (!confirm) {
      return null;
    }
    const mismatch = group.get(field)?.value !== confirm.value;
    if (mismatch) {
      confirm.setErrors({ ...confirm.errors, mismatch: true });
    } else if (confirm.errors?.['mismatch']) {
      const { mismatch: _removed, ...rest } = confirm.errors;
      confirm.setErrors(Object.keys(rest).length ? rest : null);
    }
    return null;
  };
}
