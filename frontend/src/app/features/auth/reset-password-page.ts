import { Component, ElementRef, inject, input, signal } from '@angular/core';
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

/** CU-16: definir una contraseña nueva con el enlace recibido (HU-05). */
@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-card" aria-labelledby="reset-title">
      <h1 id="reset-title">Nueva contraseña</h1>
      @if (doneMessage(); as message) {
        <p class="form-success" role="status">{{ message }}</p>
        <a class="button" routerLink="/ingresar">Ingresar</a>
      } @else if (!token()) {
        <p class="form-alert" role="alert">El enlace no es válido.</p>
        <a routerLink="/recuperar-contrasena">Pedir un enlace nuevo</a>
      } @else {
        @if (formError(); as message) {
          <div class="form-alert" role="alert">
            <p>{{ message }}</p>
            <a routerLink="/recuperar-contrasena">Pedir un enlace nuevo</a>
          </div>
        }
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="reset-password">Contraseña nueva</label>
            <p class="hint" id="reset-password-hint">Mínimo 10 caracteres.</p>
            <input
              id="reset-password"
              type="password"
              formControlName="password"
              autocomplete="new-password"
              [attr.aria-invalid]="showError(form.controls.password)"
              [attr.aria-describedby]="
                showError(form.controls.password)
                  ? 'reset-password-hint reset-password-error'
                  : 'reset-password-hint'
              "
            />
            @if (showError(form.controls.password)) {
              <p class="field-error" id="reset-password-error">
                {{ errorMessage(form.controls.password, messages.password) }}
              </p>
            }
          </div>
          <div class="field">
            <label for="reset-confirm">Repite la contraseña</label>
            <input
              id="reset-confirm"
              type="password"
              formControlName="confirmPassword"
              autocomplete="new-password"
              [attr.aria-invalid]="showError(form.controls.confirmPassword)"
              [attr.aria-describedby]="
                showError(form.controls.confirmPassword) ? 'reset-confirm-error' : null
              "
            />
            @if (showError(form.controls.confirmPassword)) {
              <p class="field-error" id="reset-confirm-error">
                {{ errorMessage(form.controls.confirmPassword, messages.confirmPassword) }}
              </p>
            }
          </div>
          <button class="button button--block" type="submit" [disabled]="submitting()">
            Guardar contraseña
          </button>
        </form>
      }
    </section>
  `,
})
export class ResetPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Token del enlace (?token=...). */
  readonly token = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      password: [
        '',
        [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH), Validators.maxLength(72)],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchingFields('password', 'confirmPassword') },
  );
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly doneMessage = signal<string | null>(null);
  protected readonly messages = {
    password: {
      required: 'Ingresa una contraseña.',
      minlength: 'La contraseña debe tener al menos 10 caracteres.',
      maxlength: 'La contraseña puede tener como máximo 72 caracteres.',
    },
    confirmPassword: {
      required: 'Repite la contraseña.',
      mismatch: 'Las contraseñas no coinciden.',
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
    this.auth.resetPassword(this.token() ?? '', this.form.controls.password.value).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.doneMessage.set(response.message);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const problem = toProblem(error);
        if (problem.code.startsWith('PASSWORD_')) {
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
