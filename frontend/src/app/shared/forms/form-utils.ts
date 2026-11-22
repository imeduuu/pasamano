import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ApiProblem } from '../../core/http/problem';

/** Mensajes en español por clave de error de validación. */
export type ErrorMessages = Readonly<Record<string, string>>;

export const PASSWORD_MIN_LENGTH = 10;

/** Un campo muestra su error solo después de que el usuario interactuó o intentó enviar. */
export function showError(control: AbstractControl | null): boolean {
  return !!control && control.invalid && (control.touched || control.dirty);
}

/** Primer mensaje aplicable; los errores del servidor (`server`) tienen prioridad. */
export function errorMessage(
  control: AbstractControl | null,
  messages: ErrorMessages,
): string | null {
  if (!control?.errors) {
    return null;
  }
  const server = control.errors['server'];
  if (typeof server === 'string') {
    return server;
  }
  for (const key of Object.keys(control.errors)) {
    if (messages[key]) {
      return messages[key];
    }
  }
  return 'Revisa este campo.';
}

/** Copia los errores por campo de Problem Details a los controles del formulario. */
export function applyServerErrors(form: FormGroup, problem: ApiProblem): boolean {
  let applied = false;
  for (const [field, message] of Object.entries(problem.fieldErrors)) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}

/** Lleva el foco al primer campo inválido para usuarios de teclado y lectores de pantalla. */
export function focusFirstInvalid(root: HTMLElement): void {
  setTimeout(() => {
    root.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  });
}

/** Valida que dos controles del grupo tengan el mismo valor (contraseña y confirmación). */
export function matchingFields(field: string, confirmation: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const confirm = group.get(confirmation);
    if (!confirm) {
      return null;
    }
    const mismatch = group.get(field)?.value !== confirm.value;
    const others = { ...(confirm.errors ?? {}) };
    delete others['mismatch'];
    if (mismatch && confirm.value) {
      confirm.setErrors({ ...others, mismatch: true });
    } else {
      confirm.setErrors(Object.keys(others).length ? others : null);
    }
    return null;
  };
}
