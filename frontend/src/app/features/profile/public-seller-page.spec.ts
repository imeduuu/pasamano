import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PublicSellerPage } from './public-seller-page';

describe('Perfil público de vendedor (HU-14)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'vendedores/:id', component: PublicSellerPage }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('muestra perfil, antigüedad, ventas y publicaciones con el título de tienda', async () => {
    const harness = await RouterTestingHarness.create('/vendedores/seller-1');
    const root = harness.routeNativeElement as HTMLElement;
    http.expectOne('/api/v1/sellers/seller-1').flush({
      storeName: 'Tienda Sur',
      activatedAt: new Date().toISOString(),
      reputation: 'Vendedor nuevo',
      completedSales: 3,
      activeListings: 1,
    });
    http
      .expectOne(
        (request) =>
          request.url === '/api/v1/sellers/seller-1/listings' && request.params.get('page') === '0',
      )
      .flush({
        results: [
          {
            id: 'listing-1',
            title: 'Cámara',
            price: 25990,
            thumbnail: null,
            region: 'RM',
            commune: 'Santiago',
            storeName: 'Tienda Sur',
            reputation: 'Vendedor nuevo',
          },
        ],
        total: 1,
        totalPages: 1,
        adjustments: [],
      });
    harness.detectChanges();
    expect(root.querySelector('h1')?.textContent).toContain('Tienda Sur');
    expect(root.textContent).toContain('Vendedor nuevo');
    expect(root.textContent).toContain('3 ventas completadas');
    expect(root.querySelector('app-listing-card h2')?.textContent).toContain('Cámara');
    expect(TestBed.inject(Title).getTitle()).toBe('Tienda Sur · Vendedor en Trato');
  });

  it('cambia de página mediante la URL y carga los resultados siguientes', async () => {
    const harness = await RouterTestingHarness.create('/vendedores/seller-1');
    http.expectOne('/api/v1/sellers/seller-1').flush({
      storeName: 'Tienda Sur',
      activatedAt: new Date().toISOString(),
      reputation: '4.5 (2 reseñas)',
      completedSales: 2,
      activeListings: 0,
    });
    http
      .expectOne(
        (request) => request.url.endsWith('/listings') && request.params.get('page') === '0',
      )
      .flush({
        results: [
          {
            id: 'listing-1',
            title: 'Cámara',
            price: 1000,
            thumbnail: null,
            region: 'RM',
            commune: 'Santiago',
            storeName: 'Tienda Sur',
            reputation: '4.5',
          },
        ],
        total: 21,
        totalPages: 2,
        adjustments: [],
      });
    harness.detectChanges();
    const root = harness.routeNativeElement as HTMLElement;
    const next = root.querySelector<HTMLButtonElement>('button:last-child');
    next?.click();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/vendedores/seller-1?page=1');
    http
      .expectOne(
        (request) => request.url.endsWith('/listings') && request.params.get('page') === '1',
      )
      .flush({ results: [], total: 21, totalPages: 2, adjustments: [] });
    harness.detectChanges();
  });
});
