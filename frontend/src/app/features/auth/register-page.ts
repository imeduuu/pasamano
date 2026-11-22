import { Component, ElementRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import {
  PASSWORD_MIN_LENGTH,
  applyServerErrors,
  errorMessage,
  focusFirstInvalid,
  matchingFields,
  showError,
} from '../../shared/forms/form-utils';

/** CU-01 Registrarse (HU-01). */
@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
      password: [
        '',
        [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH), Validators.maxLength(72)],
      ],
      confirmPassword: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]],
    },
    { validators: matchingFields('password', 'confirmPassword') },
  );
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly sentMessage = signal<string | null>(null);

  protected readonly messages = {
    name: {
      required: 'Ingresa tu nombre.',
      minlength: 'El nombre debe tener al menos 2 caracteres.',
      maxlength: 'El nombre puede tener como máximo 60 caracteres.',
    },
    email: {
      required: 'Ingresa tu correo.',
      email: 'El correo electrónico no es válido.',
      maxlength: 'El correo es demasiado largo.',
    },
    password: {
      required: 'Ingresa una contraseña.',
      minlength: 'La contraseña debe tener al menos 10 caracteres.',
      maxlength: 'La contraseña puede tener como máximo 72 caracteres.',
    },
    confirmPassword: {
      required: 'Repite la contraseña.',
      mismatch: 'Las contraseñas no coinciden.',
    },
    acceptTerms: {
      required: 'Debes aceptar los términos y la política de privacidad.',
    },
  };
  protected readonly showError = showError;
  protected readonly errorMessage = errorMessage;

  submit(): void {
    this.formError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    this.submitting.set(true);
    const { name, email, password, acceptTerms } = this.form.getRawValue();
    this.auth.register({ name, email, password, acceptTerms }).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.sentMessage.set(response.message);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const problem = toProblem(error);
        if (problem.code === 'PASSWORD_TOO_COMMON' || problem.code.startsWith('PASSWORD_')) {
          this.form.controls.password.setErrors({ server: problem.detail });
          this.form.controls.password.markAsTouched();
        } else if (!applyServerErrors(this.form, problem)) {
          this.formError.set(problem.detail);
        }
        focusFirstInvalid(this.host.nativeElement);
      },
    });
  }
}
