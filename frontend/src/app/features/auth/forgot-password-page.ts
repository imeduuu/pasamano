import { Component, ElementRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import { errorMessage, focusFirstInvalid, showError } from '../../shared/forms/form-utils';

/** CU-16 Recuperar contraseña: la respuesta es la misma exista o no el correo (HU-05). */
@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-card" aria-labelledby="forgot-title">
      <h1 id="forgot-title">Recuperar contraseña</h1>
      @if (sentMessage(); as message) {
        <div class="form-success" role="status">
          <p>{{ message }}</p>
          <p>El enlace vale por 1 hora y solo se puede usar una vez.</p>
        </div>
        <a routerLink="/ingresar">Volver a ingresar</a>
      } @else {
        <p>Escribe tu correo y te enviaremos un enlace para crear una contraseña nueva.</p>
        @if (formError(); as message) {
          <p class="form-alert" role="alert">{{ message }}</p>
        }
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="forgot-email">Correo electrónico</label>
            <input
              id="forgot-email"
              type="email"
              formControlName="email"
              autocomplete="email"
              [attr.aria-invalid]="showError(form.controls.email)"
              [attr.aria-describedby]="showError(form.controls.email) ? 'forgot-email-error' : null"
            />
            @if (showError(form.controls.email)) {
              <p class="field-error" id="forgot-email-error">
                {{ errorMessage(form.controls.email, emailMessages) }}
              </p>
            }
          </div>
          <button class="button button--block" type="submit" [disabled]="submitting()">
            Enviar enlace
          </button>
        </form>
        <div class="form-links"><a routerLink="/ingresar">Volver a ingresar</a></div>
      }
    </section>
  `,
})
export class ForgotPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });
  protected readonly submitting = signal(false);
  protected readonly sentMessage = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly emailMessages = {
    required: 'Ingresa tu correo.',
    email: 'El correo electrónico no es válido.',
  };
  protected readonly showError = showError;
  protected readonly errorMessage = errorMessage;

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    this.submitting.set(true);
    this.auth.forgotPassword(this.form.controls.email.value).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.sentMessage.set(response.message);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.formError.set(toProblem(error).detail);
      },
    });
  }
}
