import { Component, ElementRef, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';
import {
  PASSWORD_MIN_LENGTH,
  applyServerErrors,
  errorMessage,
  focusFirstInvalid,
  showError,
} from '../../shared/forms/form-utils';

interface Region {
  code: string;
  name: string;
  communes: { code: string; name: string }[];
}

/** RF-USR-01 (HU-06): ver y editar el perfil propio; RF-AUT-07: cambiar la contraseña. */
@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule],
  templateUrl: './profile-page.html',
})
export class ProfilePage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly api = inject(ApiClient);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly profileForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
    region: [''],
    commune: [''],
    photoUrl: ['', [Validators.maxLength(500), Validators.pattern(/^(https?:\/\/\S+)?$/)]],
  });
  protected readonly passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: [
      '',
      [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH), Validators.maxLength(72)],
    ],
  });

  protected readonly saving = signal(false);
  protected readonly regions = signal<Region[]>([]);
  protected readonly regionCode = signal('');
  protected readonly profileMessage = signal<string | null>(null);
  protected readonly profileError = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);

  protected readonly messages = {
    name: {
      required: 'Ingresa tu nombre.',
      minlength: 'El nombre debe tener al menos 2 caracteres.',
      maxlength: 'El nombre puede tener como máximo 60 caracteres.',
    },
    photoUrl: {
      pattern: 'La foto debe ser una URL que empiece con http:// o https://.',
      maxlength: 'La URL es demasiado larga.',
    },
    currentPassword: { required: 'Ingresa tu contraseña actual.' },
    newPassword: {
      required: 'Ingresa una contraseña nueva.',
      minlength: 'La contraseña debe tener al menos 10 caracteres.',
      maxlength: 'La contraseña puede tener como máximo 72 caracteres.',
    },
  };
  protected readonly showError = showError;
  protected readonly errorMessage = errorMessage;

  ngOnInit(): void {
    this.api.get<Region[]>('/regions').subscribe({ next: (regions) => this.regions.set(regions) });
    const current = this.auth.user();
    if (current) {
      this.fill();
    } else {
      this.auth.loadProfile().subscribe({ next: () => this.fill() });
    }
  }

  protected get communes(): Region['communes'] {
    return this.regions().find((region) => region.code === this.regionCode())?.communes ?? [];
  }

  protected selectRegion(code: string): void {
    this.regionCode.set(code);
    this.profileForm.controls.region.setValue(code);
    this.profileForm.controls.commune.setValue('');
  }

  saveProfile(): void {
    this.profileMessage.set(null);
    this.profileError.set(null);
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    this.saving.set(true);
    this.auth.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: () => {
        this.saving.set(false);
        this.profileMessage.set('Guardamos los cambios de tu perfil.');
        this.fill();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const problem = toProblem(error);
        const field = this.fieldForCode(problem.code);
        if (field) {
          this.profileForm.controls[field].setErrors({ server: problem.detail });
          this.profileForm.controls[field].markAsTouched();
        } else if (!applyServerErrors(this.profileForm, problem)) {
          this.profileError.set(problem.detail);
        }
        focusFirstInvalid(this.host.nativeElement);
      },
    });
  }

  changePassword(): void {
    this.passwordError.set(null);
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: () => void this.router.navigate(['/ingresar']),
      error: (error: unknown) => {
        const problem = toProblem(error);
        if (problem.code === 'CURRENT_PASSWORD_INCORRECT') {
          this.passwordForm.controls.currentPassword.setErrors({ server: problem.detail });
        } else if (problem.code.startsWith('PASSWORD_')) {
          this.passwordForm.controls.newPassword.setErrors({ server: problem.detail });
        } else if (!applyServerErrors(this.passwordForm, problem)) {
          this.passwordError.set(problem.detail);
        }
        this.passwordForm.markAllAsTouched();
        focusFirstInvalid(this.host.nativeElement);
      },
    });
  }

  private fill(): void {
    const user = this.auth.user();
    if (user) {
      this.profileForm.reset({
        name: user.name,
        region: user.region ?? '',
        commune: user.commune ?? '',
        photoUrl: user.photoUrl ?? '',
      });
      this.regionCode.set(user.region ?? '');
    }
  }

  private fieldForCode(code: string): 'name' | 'region' | 'commune' | 'photoUrl' | null {
    const map: Record<string, 'name' | 'region' | 'commune' | 'photoUrl'> = {
      INVALID_NAME: 'name',
      INVALID_REGION: 'region',
      INVALID_COMMUNE: 'commune',
      INVALID_PHOTO_URL: 'photoUrl',
    };
    return map[code] ?? null;
  }
}
