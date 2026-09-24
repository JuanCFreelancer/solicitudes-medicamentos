import { FormControl, FormGroup } from '@angular/forms';
import { emailFormat, fieldsMatch, notBlank, passwordStrength, phoneFormat } from './form-validators';

describe('form-validators', () => {
  describe('notBlank', () => {
    it('rechaza vacío y solo espacios', () => {
      expect(notBlank(new FormControl(''))).toEqual({ required: true });
      expect(notBlank(new FormControl('   '))).toEqual({ required: true });
      expect(notBlank(new FormControl(null))).toEqual({ required: true });
    });

    it('acepta texto con contenido', () => {
      expect(notBlank(new FormControl(' ORD-1 '))).toBeNull();
    });
  });

  describe('emailFormat', () => {
    it('acepta correos válidos y vacío (la obligatoriedad la decide otro validador)', () => {
      expect(emailFormat(new FormControl('ana@correo.com'))).toBeNull();
      expect(emailFormat(new FormControl(''))).toBeNull();
    });

    it('rechaza correos sin arroba o sin dominio con punto', () => {
      expect(emailFormat(new FormControl('ana'))).toEqual({ email: true });
      expect(emailFormat(new FormControl('ana@correo'))).toEqual({ email: true });
    });
  });

  describe('phoneFormat', () => {
    it('acepta separadores habituales y prefijo +', () => {
      expect(phoneFormat(new FormControl('3001234567'))).toBeNull();
      expect(phoneFormat(new FormControl('300 123-4567'))).toBeNull();
      expect(phoneFormat(new FormControl('+57 (300) 1234567'))).toBeNull();
    });

    it('rechaza letras y longitudes fuera de 7–15 dígitos', () => {
      expect(phoneFormat(new FormControl('abc1234567'))).toEqual({ phone: true });
      expect(phoneFormat(new FormControl('123456'))).toEqual({ phone: true });
      expect(phoneFormat(new FormControl('1234567890123456'))).toEqual({ phone: true });
    });
  });

  describe('passwordStrength', () => {
    it('exige al menos una letra y un número', () => {
      expect(passwordStrength(new FormControl('Clave1234'))).toBeNull();
      expect(passwordStrength(new FormControl('sololetras'))).toEqual({ passwordStrength: true });
      expect(passwordStrength(new FormControl('12345678'))).toEqual({ passwordStrength: true });
    });
  });

  describe('fieldsMatch', () => {
    const build = (a: string, b: string) =>
      new FormGroup({ password: new FormControl(a), confirm: new FormControl(b) }, { validators: fieldsMatch('password', 'confirm') });

    it('marca error en el campo de confirmación cuando no coinciden y lo quita al coincidir', () => {
      const group = build('Clave1234', 'otra');
      expect(group.controls['confirm'].errors).toEqual({ mismatch: true });

      group.controls['confirm'].setValue('Clave1234');
      expect(group.controls['confirm'].errors).toBeNull();
    });
  });
});
