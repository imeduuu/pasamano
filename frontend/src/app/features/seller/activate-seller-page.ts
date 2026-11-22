import { Component, ElementRef, inject, signal } from '@angular/core';
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

/** HU-07 / CU-15: activa el perfil vendedor y acepta los términos de venta. */
@Component({
  selector: 'app-activate-seller-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './activate-seller-page.html',
})
export class ActivateSellerPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    storeName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(40)]],
    acceptSalesTerms: [false, Validators.requiredTrue],
  });
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly messages = {
    required: 'Ingresa el nombre de tu tienda.',
    minlength: 'El nombre de tienda debe tener entre 3 y 40 caracteres.',
    maxlength: 'El nombre de tienda debe tener entre 3 y 40 caracteres.',
    requiredTrue: 'Debes aceptar los términos de venta.',
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
    this.auth.activateSellerProfile(this.form.controls.storeName.value.trim()).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl('/publicar-producto');
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const problem = toProblem(error);
        if (problem.code === 'STORE_NAME_TAKEN') {
          this.form.controls.storeName.setErrors({ server: problem.detail });
          this.form.controls.storeName.markAsTouched();
        } else if (!applyServerErrors(this.form, problem)) {
          this.formError.set(problem.detail);
        }
        focusFirstInvalid(this.host.nativeElement);
      },
    });
  }
}
