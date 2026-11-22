import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';

describe('App (prueba de humo)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('muestra el layout con el aviso de demostración (RNF-LEG-03)', async () => {
    const harness = await RouterTestingHarness.create('/');
    const root = harness.fixture.nativeElement as HTMLElement;

    expect(root.querySelector('[role="note"]')?.textContent).toContain(
      'Proyecto de demostración · pagos en ambiente de pruebas de Transbank',
    );
    expect(root.querySelector('header')).toBeTruthy();
    expect(root.querySelector('main#contenido')).toBeTruthy();
    expect(root.querySelector('footer')).toBeTruthy();
  });

  it('sin sesión muestra los accesos para ingresar y crear cuenta', async () => {
    const harness = await RouterTestingHarness.create('/');
    const nav = (harness.fixture.nativeElement as HTMLElement).querySelector('nav');

    expect(nav?.textContent).toContain('Ingresar');
    expect(nav?.textContent).toContain('Crear cuenta');
  });

  it('muestra la página de inicio en la ruta raíz', async () => {
    const harness = await RouterTestingHarness.create('/');

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain(
      'Compra y vende entre personas',
    );
  });

  it('muestra la página 404 para rutas desconocidas', async () => {
    const harness = await RouterTestingHarness.create('/no-existe');

    expect(harness.routeNativeElement?.textContent).toContain('No encontramos esta página');
  });

  it('la ruta del perfil exige sesión y redirige al login con la ruta de retorno', async () => {
    const harness = await RouterTestingHarness.create('/perfil');

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Ingresar');
  });
});
