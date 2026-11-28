import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SearchPage } from './search-page';

describe('BÃºsqueda de publicaciones (HU-12/HU-13, CU-04 4a)', () => {
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

  function flushCatalog(): void {
    http.expectOne('/api/v1/categories').flush([]);
    http.expectOne('/api/v1/regions').flush([]);
  }

  it('lee la bÃºsqueda desde la URL y anuncia resultados con ubicaciÃ³n y reputaciÃ³n', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=cÃ¡mara&page=1');
    flushCatalog();
    http
      .expectOne(
        (request) =>
          request.url === '/api/v1/listings' &&
          request.params.get('q') === 'cÃ¡mara' &&
          request.params.get('page') === '1',
      )
      .flush({
        results: [
          {
            id: 'one',
            title: 'CÃ¡mara anÃ¡loga',
            price: 25990,
            thumbnail: '/camera.jpg',
            region: 'RegiÃ³n Metropolitana',
            commune: 'Santiago',
            storeName: 'Tienda Sur',
            reputation: '4.8 (12 reseÃ±as)',
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
    expect(root.textContent).toContain('RegiÃ³n Metropolitana · Santiago');
    expect(root.textContent).toContain('4.8 (12 reseÃ±as)');
  });

  it('muestra carga, sugerencias en vacÃ­o y comunica errores', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=xyz');
    flushCatalog();
    expect(document.body.textContent).toContain('Buscando publicaciones');
    http
      .expectOne(
        (request) => request.url === '/api/v1/listings' && request.params.get('q') === 'xyz',
      )
      .flush({ results: [], total: 0, totalPages: 0, adjustments: [] });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Revisa la ortografÃ­a');
    expect(harness.routeNativeElement?.textContent).toContain('Quita algunos filtros');
  });

  it('presenta el error de la API en un anuncio accesible', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=error');
    flushCatalog();
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

  it('sincroniza filtros de la URL con la API y muestra los ajustes informados', async () => {
    const harness = await RouterTestingHarness.create(
      '/buscar?q=libro&category=leaf&minPrice=900&maxPrice=100&condition=USADO&region=13&minRating=4&sort=price_asc',
    );
    http
      .expectOne('/api/v1/categories')
      .flush([
        { id: 'root', name: 'Libros', children: [{ id: 'leaf', name: 'Novelas', children: [] }] },
      ]);
    http.expectOne('/api/v1/regions').flush([{ code: '13', name: 'Metropolitana', communes: [] }]);
    const search = http.expectOne((request) => request.url === '/api/v1/listings');
    expect(search.request.params.get('category')).toBe('leaf');
    expect(search.request.params.get('minPrice')).toBe('900');
    expect(search.request.params.get('maxPrice')).toBe('100');
    expect(search.request.params.get('condition')).toBe('USADO');
    expect(search.request.params.get('region')).toBe('13');
    expect(search.request.params.get('minRating')).toBe('4');
    expect(search.request.params.get('sort')).toBe('price_asc');
    search.flush({ results: [], total: 0, totalPages: 0, adjustments: ['PRICE_RANGE_RESET'] });
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="status"]')?.textContent).toContain(
      'mayor que el mÃ¡ximo',
    );
    expect(
      harness.routeNativeElement?.querySelector('[data-active-filter-count]')?.textContent,
    ).toContain('6');
  });

  it('al aplicar filtros actualiza la URL y restablece la pÃ¡gina', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=libro&page=2');
    flushCatalog();
    http
      .expectOne((request) => request.url === '/api/v1/listings')
      .flush({
        results: [],
        total: 0,
        totalPages: 0,
        adjustments: [],
      });
    harness.detectChanges();
    const root = harness.routeNativeElement as HTMLElement;
    root.querySelector<HTMLSelectElement>('#sort')!.value = 'price_asc';
    root.querySelector<HTMLSelectElement>('#sort')!.dispatchEvent(new Event('change'));
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/buscar?q=libro&sort=price_asc');
    http
      .expectOne(
        (request) =>
          request.url === '/api/v1/listings' && request.params.get('sort') === 'price_asc',
      )
      .flush({ results: [], total: 0, totalPages: 0, adjustments: [] });
  });

  it('expone en mÃ³vil un botÃ³n de filtros con estado accesible', async () => {
    const harness = await RouterTestingHarness.create('/buscar?condition=NUEVO');
    flushCatalog();
    http
      .expectOne((request) => request.url === '/api/v1/listings')
      .flush({ results: [], total: 0, totalPages: 0, adjustments: [] });
    harness.detectChanges();
    const button =
      harness.routeNativeElement?.querySelector<HTMLButtonElement>('[data-filter-toggle]');
    expect(button?.textContent).toContain('Filtros');
    expect(button?.textContent).toContain('1');
    button?.click();
    harness.detectChanges();
    expect(button?.getAttribute('aria-expanded')).toBe('true');
  });

  it('al cambiar de pÃ¡gina actualiza la URL y devuelve el foco al tÃ­tulo', async () => {
    const harness = await RouterTestingHarness.create('/buscar?q=libro');
    flushCatalog();
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
            storeName: 'LibrerÃ­a',
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
