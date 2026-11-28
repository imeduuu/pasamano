import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Title, Meta } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { UserProfile } from '../../core/auth/auth.models';
import { ListingDetailPage } from './listing-detail-page';

describe('Detalle público de publicación (RF-BUS-05)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ListingDetailPage>;
  let root: HTMLElement;
  const authUser = signal<UserProfile | null>(null);
  let authenticated = false;

  beforeEach(() => {
    authenticated = false;
    authUser.set(null);
    TestBed.configureTestingModule({
      imports: [ListingDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { isAuthenticated: () => authenticated, user: authUser },
        },
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

  it('muestra cantidad y agrega al carrito; visitante vuelve al producto después del login (HU-15)', () => {
    loadListing();
    expect(root.querySelector('#listing-quantity')).not.toBeNull();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    root.querySelector<HTMLButtonElement>('.add-to-cart')!.click();
    expect(navigate).toHaveBeenCalledWith(['/ingresar'], {
      queryParams: { returnUrl: '/producto/listing-1' },
    });
    http.expectNone('/api/v1/cart/items/listing-1');
  });

  it('oculta la compra en las publicaciones propias y en las no disponibles', () => {
    authUser.set({
      id: 'seller-1',
      email: 'seller@ejemplo.cl',
      name: 'Vendedor',
      region: null,
      commune: null,
      photoUrl: null,
      status: 'ACTIVE',
      roles: ['USER'],
      seller: true,
      createdAt: '2026-01-01T00:00:00Z',
    });
    authenticated = true;
    loadListing();
    expect(root.querySelector('.add-to-cart')).toBeNull();

    fixture.destroy();
    authUser.set(null);
    authenticated = false;
    fixture = TestBed.createComponent(ListingDetailPage);
    root = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    loadListing('PAUSADA', 3);
    expect(root.querySelector('.add-to-cart')).toBeNull();
  });

  it('ofrece la cantidad máxima tras un conflicto de stock (CU-05 2b)', () => {
    authenticated = true;
    authUser.set({
      id: 'buyer-1',
      email: 'buyer@ejemplo.cl',
      name: 'Comprador',
      region: null,
      commune: null,
      photoUrl: null,
      status: 'ACTIVE',
      roles: ['USER'],
      seller: false,
      createdAt: '2026-01-01T00:00:00Z',
    });
    loadListing();
    root.querySelector<HTMLButtonElement>('.add-to-cart')!.click();
    http
      .expectOne('/api/v1/cart/items/listing-1')
      .flush(
        { code: 'INSUFFICIENT_STOCK', detail: 'Stock insuficiente.', maximumAvailable: 1 },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();
    const offer = root.querySelector<HTMLButtonElement>('.maximum-button');
    expect(offer?.textContent).toContain('Agregar 1 disponibles');
    offer?.click();
    const retry = http.expectOne('/api/v1/cart/items/listing-1');
    expect(retry.request.body).toEqual({ quantity: 1 });
    retry.flush({ sellers: [], total: 0, warnings: [] });
    fixture.detectChanges();
  });
});
