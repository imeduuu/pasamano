import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SearchPage } from './search-page';

describe('Búsqueda de publicaciones (HU-12, CU-04 4a)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'buscar', component: SearchPage }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lee la búsqueda desde la URL y anuncia resultados con ubicación y reputación', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=cámara&page=1');
    http
      .expectOne(
        (request) =>
          request.url === '/api/v1/listings' &&
          request.params.get('q') === 'cámara' &&
          request.params.get('page') === '1',
      )
      .flush({
        results: [
          {
            id: 'one',
            title: 'Cámara análoga',
            price: 25990,
            thumbnail: '/camera.jpg',
            region: 'Región Metropolitana',
            commune: 'Santiago',
            storeName: 'Tienda Sur',
            reputation: '4.8 (12 reseñas)',
          },
        ],
        total: 21,
        totalPages: 2,
        adjustments: [],
      });
    harness.detectChanges();
    const root = harness.routeNativeElement as HTMLElement;
    expect(root.querySelector('[role="search"]')).toBeTruthy();
    expect(root.querySelector('[aria-live="polite"]')?.textContent).toContain('21 resultados');
    expect(root.textContent).toContain('$25.990');
    expect(root.textContent).toContain('Región Metropolitana · Santiago');
    expect(root.textContent).toContain('4.8 (12 reseñas)');
  });

  it('muestra carga, sugerencias en vacío y comunica errores', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=xyz');
    expect(document.body.textContent).toContain('Buscando publicaciones');
    http
      .expectOne(
        (request) => request.url === '/api/v1/listings' && request.params.get('q') === 'xyz',
      )
      .flush({ results: [], total: 0, totalPages: 0, adjustments: [] });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Revisa la ortografía');
    expect(harness.routeNativeElement?.textContent).toContain('Quita algunos filtros');
  });

  it('presenta el error de la API en un anuncio accesible', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=error');
    http
      .expectOne((request) => request.url === '/api/v1/listings')
      .flush(
        { detail: 'Servicio no disponible' },
        { status: 503, statusText: 'Service Unavailable' },
      );
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')?.textContent).toContain(
      'Servicio no disponible',
    );
  });

  it('al cambiar de página actualiza la URL y devuelve el foco al título', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=libro');
    http
      .expectOne(
        (request) => request.url === '/api/v1/listings' && request.params.get('page') === '0',
      )
      .flush({
        results: [
          {
            id: 'book',
            title: 'Libro',
            price: 1000,
            thumbnail: null,
            region: '13',
            commune: '13101',
            storeName: 'Librería',
            reputation: 'Vendedor nuevo',
          },
        ],
        total: 21,
        totalPages: 2,
        adjustments: [],
      });
    harness.detectChanges();
    const next = harness.routeNativeElement?.querySelector<HTMLButtonElement>('[data-next-page]');
    next?.click();
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/buscar?q=libro&page=1');
    http
      .expectOne(
        (request) => request.url === '/api/v1/listings' && request.params.get('page') === '1',
      )
      .flush({ results: [], total: 21, totalPages: 2, adjustments: [] });
    harness.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.activeElement).toBe(harness.routeNativeElement?.querySelector('h1'));
  });
});
