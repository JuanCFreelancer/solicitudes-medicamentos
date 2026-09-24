import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { toApiError } from '../../../core/utils/api-error';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { emailFormat, notBlank } from '../../../shared/validators/form-validators';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AlertComponent, FieldErrorComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // Parámetros de la URL (withComponentInputBinding): ?registered=1 &expired=1 &returnUrl=...
  readonly registered = input<string>();
  readonly expired = input<string>();
  readonly returnUrl = input<string>();

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.group({
    email: ['', [notBlank, emailFormat]],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.safeReturnUrl()),
      error: (error: unknown) => {
        this.errorMessage.set(toApiError(error).message);
        this.loading.set(false);
      },
    });
  }

  /** Solo se aceptan rutas internas: evita redirecciones abiertas (?returnUrl=https://sitio-malo). */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/solicitudes';
  }
}
