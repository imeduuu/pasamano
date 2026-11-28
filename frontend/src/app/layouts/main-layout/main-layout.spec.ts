import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../features/cart/cart.service';
import { MainLayout } from './main-layout';

describe('Encabezado del carrito (HU-15)', () => {
  let fixture: ComponentFixture<MainLayout>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAuthenticated: () => false, user: signal(null) } },
      ],
    });
    fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
  });

  it('muestra el contador actualizado y lo anuncia con su nombre accesible', () => {
    TestBed.inject(CartService).cart.set({
      sellers: [
        {
          sellerId: 'seller-1',
          subtotal: 2000,
          items: [
            {
              listingId: 'item-1',
              title: 'Producto',
              quantity: 2,
              unitPrice: 1000,
              subtotal: 2000,
              available: true,
              warnings: [],
            },
          ],
        },
      ],
      total: 2000,
      warnings: [],
    });
    fixture.detectChanges();

    const count = fixture.nativeElement.querySelector('.cart-count') as HTMLElement;
    expect(count.textContent?.trim()).toBe('2');
    expect(count.getAttribute('aria-label')).toBe('2 artículos');
  });
});
