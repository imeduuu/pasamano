import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { LoginPage } from './login-page';
import { RegisterPage } from './register-page';

function setValue(root: HTMLElement, selector: string, value: string): void {
  const input = root.querySelector<HTMLInputElement>(selector)!;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

function submit(root: HTMLElement): void {
  root.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
}

describe('Pantallas de autenticación', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('Ingresar', () => {
    let fixture: ComponentFixture<LoginPage>;
    let root: HTMLElement;

    beforeEach(async () => {
      fixture = TestBed.createComponent(LoginPage);
      root = fixture.nativeElement as HTMLElement;
      await fixture.whenStable();
    });

    it('cada campo tiene su etiqueta asociada', () => {
      for (const input of Array.from(root.querySelectorAll('input'))) {
        expect(root.querySelector(`label[for="${input.id}"]`)).toBeTruthy();
      }
    });

    it('al enviar vacío muestra errores asociados a cada campo y no llama a la API', async () => {
      submit(root);
      fixture.detectChanges();
      await fixture.whenStable();

      const email = root.querySelector('#login-email')!;
      expect(email.getAttribute('aria-invalid')).toBe('true');
      expect(email.getAttribute('aria-describedby')).toBe('login-email-error');
      expect(root.querySelector('#login-email-error')?.textContent).toContain('Ingresa tu correo');
      expect(root.querySelector('#login-password-error')).toBeTruthy();
      http.expectNone('/api/v1/auth/login');
    });

    it('con credenciales incorrectas muestra el mensaje genérico como alerta', async () => {
      setValue(root, '#login-email', 'ana@ejemplo.cl');
      setValue(root, '#login-password', 'equivocada');
      submit(root);

      http
        .expectOne('/api/v1/auth/login')
        .flush(
          { code: 'INVALID_CREDENTIALS', detail: 'Correo o contraseña incorrectos.' },
          { status: 401, statusText: 'Unauthorized' },
        );
      fixture.detectChanges();

      expect(root.querySelector('[role="alert"]')?.textContent).toContain(
        'Correo o contraseña incorrectos.',
      );
    });

    it('si la cuenta no está verificada ofrece reenviar el correo', async () => {
      setValue(root, '#login-email', 'ana@ejemplo.cl');
      setValue(root, '#login-password', 'caballo-bateria-grapa');
      submit(root);
      http
        .expectOne('/api/v1/auth/login')
        .flush(
          { code: 'EMAIL_NOT_VERIFIED', detail: 'Debes verificar tu correo.' },
          { status: 403, statusText: 'Forbidden' },
        );
      fixture.detectChanges();

      const resend = Array.from(root.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Reenviar'),
      )!;
      resend.click();
      const request = http.expectOne('/api/v1/auth/verify/resend');
      expect(request.request.body).toEqual({ email: 'ana@ejemplo.cl' });
      request.flush({ message: 'Te enviamos un correo.' });
      fixture.detectChanges();

      expect(root.querySelector('[role="status"]')?.textContent).toContain(
        'Te enviamos un correo.',
      );
    });

    it('al ingresar vuelve a la página desde la que venía', async () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      fixture.componentRef.setInput('returnUrl', '/perfil?tab=datos');
      setValue(root, '#login-email', 'ana@ejemplo.cl');
      setValue(root, '#login-password', 'caballo-bateria-grapa');
      submit(root);

      http.expectOne('/api/v1/auth/login').flush({ accessToken: 'jwt', expiresIn: 900 });
      http.expectOne('/api/v1/me').flush({ id: '1', name: 'Ana' });

      expect(navigate).toHaveBeenCalledWith('/perfil?tab=datos');
    });

    it('ignora rutas de retorno externas para evitar redirecciones abiertas', async () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      fixture.componentRef.setInput('returnUrl', '//sitio-malicioso.example');
      setValue(root, '#login-email', 'ana@ejemplo.cl');
      setValue(root, '#login-password', 'caballo-bateria-grapa');
      submit(root);

      http.expectOne('/api/v1/auth/login').flush({ accessToken: 'jwt', expiresIn: 900 });
      http.expectOne('/api/v1/me').flush({ id: '1', name: 'Ana' });

      expect(navigate).toHaveBeenCalledWith('/perfil');
    });
  });

  describe('Crear cuenta', () => {
    let fixture: ComponentFixture<RegisterPage>;
    let root: HTMLElement;

    beforeEach(async () => {
      fixture = TestBed.createComponent(RegisterPage);
      root = fixture.nativeElement as HTMLElement;
      await fixture.whenStable();
    });

    function fill(password = 'caballo-bateria-grapa', confirm = password): void {
      setValue(root, '#register-name', 'Ana Pérez');
      setValue(root, '#register-email', 'ana@ejemplo.cl');
      setValue(root, '#register-password', password);
      setValue(root, '#register-confirm', confirm);
    }

    it('exige contraseña de 10 caracteres, confirmación igual y aceptar los términos', () => {
      fill('corta', 'distinta');
      submit(root);
      fixture.detectChanges();

      expect(root.querySelector('#register-password-error')?.textContent).toContain(
        'al menos 10 caracteres',
      );
      expect(root.querySelector('#register-confirm-error')?.textContent).toContain('no coinciden');
      expect(root.querySelector('#register-terms-error')?.textContent).toContain('Debes aceptar');
      expect(root.querySelector('#register-password')?.getAttribute('aria-describedby')).toBe(
        'register-password-hint register-password-error',
      );
      http.expectNone('/api/v1/auth/register');
    });

    it('envía el registro y muestra el aviso de verificación', () => {
      fill();
      root.querySelector<HTMLInputElement>('#register-terms')!.click();
      submit(root);

      const request = http.expectOne('/api/v1/auth/register');
      expect(request.request.body).toEqual({
        name: 'Ana Pérez',
        email: 'ana@ejemplo.cl',
        password: 'caballo-bateria-grapa',
        acceptTerms: true,
      });
      request.flush({ message: 'Te enviamos un correo para verificar tu cuenta.' });
      fixture.detectChanges();

      expect(root.querySelector('[role="status"]')?.textContent).toContain('Te enviamos un correo');
    });

    it('muestra junto al campo el rechazo de una contraseña común', () => {
      fill('password1234');
      root.querySelector<HTMLInputElement>('#register-terms')!.click();
      submit(root);

      http
        .expectOne('/api/v1/auth/register')
        .flush(
          { code: 'PASSWORD_TOO_COMMON', detail: 'Esa contraseña es demasiado común.' },
          { status: 400, statusText: 'Bad Request' },
        );
      fixture.detectChanges();

      expect(root.querySelector('#register-password-error')?.textContent).toContain(
        'demasiado común',
      );
    });

    it('muestra los errores de validación del servidor junto a cada campo', () => {
      fill();
      root.querySelector<HTMLInputElement>('#register-terms')!.click();
      submit(root);

      http.expectOne('/api/v1/auth/register').flush(
        {
          code: 'VALIDATION_ERROR',
          detail: 'Algunos datos no son válidos.',
          errors: [{ field: 'email', message: 'El correo electrónico no es válido.' }],
        },
        { status: 400, statusText: 'Bad Request' },
      );
      fixture.detectChanges();

      expect(root.querySelector('#register-email-error')?.textContent).toContain('no es válido');
    });
  });
});
