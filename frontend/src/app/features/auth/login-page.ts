import { Component, ElementRef, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import {
  applyServerErrors,
  errorMessage,
  focusFirstInvalid,
  showError,
} from '../../shared/forms/form-utils';

/** CU-02 Iniciar sesión (HU-03). */
@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Ruta a la que volver después de ingresar (desde la query string). */
  readonly returnUrl = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly canResendVerification = signal(false);
  protected readonly notice = signal<string | null>(null);

  protected readonly messages = {
    email: { required: 'Ingresa tu correo.', email: 'El correo electrónico no es válido.' },
    password: { required: 'Ingresa tu contraseña.' },
  };
  protected readonly showError = showError;
  protected readonly errorMessage = errorMessage;

  submit(): void {
    this.formError.set(null);
    this.notice.set(null);
    this.canResendVerification.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    this.submitting.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.safeReturnUrl());
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const problem = toProblem(error);
        if (!applyServerErrors(this.form, problem)) {
          this.formError.set(problem.detail);
        }
        this.canResendVerification.set(problem.code === 'EMAIL_NOT_VERIFIED');
        focusFirstInvalid(this.host.nativeElement);
      },
    });
  }

  resendVerification(): void {
    this.auth.resendVerification(this.form.controls.email.value).subscribe({
      next: (response) => {
        this.canResendVerification.set(false);
        this.formError.set(null);
        this.notice.set(response.message);
      },
      error: (error: unknown) => this.formError.set(toProblem(error).detail),
    });
  }

  /** Solo rutas internas, para evitar redirecciones abiertas. */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/perfil';
  }
}
