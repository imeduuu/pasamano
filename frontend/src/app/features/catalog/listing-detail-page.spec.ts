import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Title, Meta } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { ListingDetailPage } from './listing-detail-page';

describe('Detalle público de publicación (RF-BUS-05)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ListingDetailPage>;
  let root: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListingDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'listing-1' } } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ListingDetailPage);
    root = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function loadListing(status = 'ACTIVA', stock = 2): void {
    http.expectOne('/api/v1/listings/listing-1').flush({
      id: 'listing-1',
      title: 'Cámara análoga',
      description: 'En buen estado',
      price: 25990,
      stock,
      condition: 'USADO',
      region: '13',
      commune: '13101',
      status,
      sellerId: 'seller-1',
      storeName: 'Vendedor nuevo',
      images: [
        {
          url: 'https://images.test/camera.jpg',
          thumbnailUrl: 'https://images.test/camera-thumb.jpg',
        },
        { url: 'https://images.test/side.jpg', thumbnailUrl: 'https://images.test/side-thumb.jpg' },
      ],
    });
    http.expectOne('/api/v1/regions').flush([
      {
        code: '13',
        name: 'Región Metropolitana',
        communes: [{ code: '13101', name: 'Santiago' }],
      },
    ]);
    fixture.detectChanges();
  }

  it('presenta datos de la publicación, perfil del vendedor y metadatos dinámicos', () => {
    loadListing();
    expect(root.querySelector('h1')?.textContent).toContain('Cámara análoga');
    expect(root.textContent).toContain('$25.990');
    expect(root.textContent).toContain('Usado');
    expect(root.textContent).toContain('Región Metropolitana');
    expect(root.textContent).toContain('Santiago');
    expect(root.textContent).toContain('2 disponibles');
    expect(root.textContent).toContain('Vendedor nuevo');
    expect(
      root.querySelector<HTMLAnchorElement>('[data-seller-profile]')?.getAttribute('href'),
    ).toBe('/vendedores/seller-1');
    expect(root.querySelectorAll('[data-thumbnail]').length).toBe(2);
    expect(TestBed.inject(Title).getTitle()).toBe('Cámara análoga · Trato');
    expect(TestBed.inject(Meta).getTag('name="description"')?.content).toContain('Cámara análoga');
  });

  it('permite navegar las miniaturas con flechas del teclado y muestra indisponibilidad', () => {
    loadListing('AGOTADA', 0);
    const first = root.querySelectorAll<HTMLButtonElement>('[data-thumbnail]')[0];
    first.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    expect(root.querySelector<HTMLImageElement>('[data-main-image]')?.src).toContain('side.jpg');
    expect(root.textContent).toContain('No disponible');
    expect(root.textContent).not.toContain('0 disponibles');
  });
});
