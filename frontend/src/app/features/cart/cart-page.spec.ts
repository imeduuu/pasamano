import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartPage } from './cart-page';

describe('Carrito de HU-15 y CU-05', () => {
  let fixture: ComponentFixture<CartPage>;
  let http: HttpTestingController;
  let root: HTMLElement;

  const cart = {
    sellers: [
      {
        sellerId: 'seller-1',
        subtotal: 30000,
        items: [
          {
            listingId: 'listing-1',
            title: 'Cámara análoga',
            quantity: 2,
            unitPrice: 15000,
            subtotal: 30000,
            available: true,
            warnings: ['PRICE_CHANGED'],
          },
          {
            listingId: 'listing-2',
            title: 'Lente usado',
            quantity: 1,
            unitPrice: 10000,
            subtotal: 10000,
            available: false,
            warnings: ['PAUSED'],
          },
        ],
      },
    ],
    total: 30000,
    warnings: ['PRICE_CHANGED', 'PAUSED'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CartPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CartPage);
    root = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    http.expectOne('/api/v1/cart').flush(cart);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('agrupa productos, muestra advertencias y excluye no disponibles del total', () => {
    expect(root.textContent).toContain('Vendedor 1');
    expect(root.textContent).toContain('Cámara análoga');
    expect(root.textContent).toContain('El precio cambió');
    expect(root.textContent).toContain('Esta publicación está pausada');
    expect(root.textContent).toContain('$30.000');
    expect(root.querySelector('[aria-live="polite"]')?.textContent).toContain('$30.000');
    expect(root.querySelectorAll('input[type="number"]').length).toBe(1);
  });

  it('actualiza cantidad y total con la respuesta del servidor', () => {
    const input = root.querySelector<HTMLInputElement>('input[type="number"]')!;
    input.value = '1';
    input.dispatchEvent(new Event('change', { bubbles: true }));
    const request = http.expectOne('/api/v1/cart/items/listing-1');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ quantity: 1 });
    request.flush({ ...cart, total: 15000 });
    fixture.detectChanges();
    expect(root.querySelector('[aria-live="polite"]')?.textContent).toContain('$15.000');
  });

  it('ofrece agregar el máximo al recibir INSUFFICIENT_STOCK (CU-05 2b)', () => {
    const input = root.querySelector<HTMLInputElement>('input[type="number"]')!;
    input.value = '4';
    input.dispatchEvent(new Event('change', { bubbles: true }));
    http
      .expectOne('/api/v1/cart/items/listing-1')
      .flush(
        { code: 'INSUFFICIENT_STOCK', detail: 'Stock insuficiente.', maximumAvailable: 2 },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();
    const offer = root.querySelector<HTMLButtonElement>('.maximum-button');
    expect(offer?.textContent).toContain('Agregar 2 disponibles');
    offer?.click();
    const retry = http.expectOne('/api/v1/cart/items/listing-1');
    expect(retry.request.body).toEqual({ quantity: 2 });
    retry.flush(cart);
    fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')).toBeNull();
  });

  it('quita productos con DELETE y sincroniza el contador', () => {
    root
      .querySelector<HTMLButtonElement>('[aria-label="Quitar Cámara análoga del carrito"]')!
      .click();
    const request = http.expectOne('/api/v1/cart/items/listing-1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
    fixture.detectChanges();
    expect(root.textContent).not.toContain('Cámara análoga');
  });
});
