import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import { errorMessage, showError } from '../../shared/forms/form-utils';

type VerifyState = 'verifying' | 'verified' | 'failed';

/** CU-01 pasos 7-8 y 8a: verificar el correo con el enlace o pedir uno nuevo (HU-02). */
@Component({
  selector: 'app-verify-email-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verify-email-page.html',
})
export class VerifyEmailPage implements OnInit {
  private readonly auth = inject(AuthService);

  /** Token del enlace (?token=...). */
  readonly token = input<string>();

  protected readonly state = signal<VerifyState>('verifying');
  protected readonly message = signal<string>('');
  protected readonly resendMessage = signal<string | null>(null);
  protected readonly resendForm = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });
  protected readonly emailMessages = {
    required: 'Ingresa tu correo.',
    email: 'El correo electrónico no es válido.',
  };
  protected readonly showError = showError;
  protected readonly errorMessage = errorMessage;

  ngOnInit(): void {
    const token = this.token();
    if (!token) {
      this.state.set('failed');
      this.message.set('El enlace no es válido.');
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: (response) => {
        this.state.set('verified');
        this.message.set(response.message);
      },
      error: (error: unknown) => {
        this.state.set('failed');
        this.message.set(toProblem(error).detail);
      },
    });
  }

  resend(): void {
    if (this.resendForm.invalid) {
      this.resendForm.markAllAsTouched();
      return;
    }
    this.auth.resendVerification(this.resendForm.controls.email.value).subscribe({
      next: (response) => this.resendMessage.set(response.message),
      error: (error: unknown) => this.resendMessage.set(toProblem(error).detail),
    });
  }
}
