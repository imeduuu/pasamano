import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiClient } from '../../core/http/api-client';
import { CartView } from './cart.models';

/** Estado compartido de HU-15 para el contador y las pantallas del carrito. */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly api = inject(ApiClient);
  readonly cart = signal<CartView | null>(null);
  readonly itemCount = computed(
    () =>
      this.cart()?.sellers.reduce(
        (count, seller) => count + seller.items.reduce((sum, item) => sum + item.quantity, 0),
        0,
      ) ?? 0,
  );

  refresh(): Observable<CartView> {
    return this.api.get<CartView>('/cart').pipe(tap((cart) => this.cart.set(cart)));
  }

  put(listingId: string, quantity: number): Observable<CartView> {
    return this.api
      .put<CartView>(`/cart/items/${listingId}`, { quantity })
      .pipe(tap((cart) => this.cart.set(cart)));
  }

  remove(listingId: string): Observable<void> {
    return this.api.delete<void>(`/cart/items/${listingId}`).pipe(
      tap(() =>
        this.cart.update((cart) =>
          cart
            ? this.withTotals({
                ...cart,
                sellers: cart.sellers
                  .map((seller) => ({
                    ...seller,
                    items: seller.items.filter((item) => item.listingId !== listingId),
                  }))
                  .filter((seller) => seller.items.length > 0),
              })
            : null,
        ),
      ),
    );
  }

  clear(): void {
    this.cart.set(null);
  }

  private withTotals(cart: CartView): CartView {
    const sellers = cart.sellers.map((seller) => {
      const availableItems = seller.items.filter((item) => item.available);
      return {
        ...seller,
        subtotal: availableItems.reduce((sum, item) => sum + item.subtotal, 0),
      };
    });
    return {
      ...cart,
      sellers,
      total: sellers.reduce((sum, seller) => sum + seller.subtotal, 0),
      warnings: sellers.flatMap((seller) => seller.items.flatMap((item) => item.warnings)),
    };
  }
}
