import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { toApiError } from '../../../core/utils/api-error';
import { applyServerErrors } from '../../../core/utils/server-errors';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { emailFormat, fieldsMatch, notBlank, passwordStrength } from '../../../shared/validators/form-validators';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, AlertComponent, FieldErrorComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.group(
    {
      nombre: ['', [notBlank, Validators.maxLength(100)]],
      email: ['', [notBlank, emailFormat, Validators.maxLength(254)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72), passwordStrength]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: fieldsMatch('password', 'confirmPassword') },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    const { nombre, email, password } = this.form.getRawValue();
    this.auth.register({ nombre, email, password }).subscribe({
      next: () => void this.router.navigate(['/login'], { queryParams: { registered: 1 } }),
      error: (error: unknown) => {
        const apiError = toApiError(error);
        // Un 409 (correo duplicado) o 400 con campos: se muestran junto al campo si es posible.
        if (!applyServerErrors(this.form, apiError.fieldErrors)) {
          this.errorMessage.set(apiError.message);
        }
        this.loading.set(false);
      },
    });
  }
}
