import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toProblem } from '../../core/http/problem';
import { CartItem } from './cart.models';
import { CartService } from './cart.service';

const WARNING_LABELS: Record<string, string> = {
  PRICE_CHANGED: 'El precio cambió desde que agregaste este producto.',
  SOLD_OUT: 'Esta publicación está agotada y no se incluye en el total.',
  PAUSED: 'Esta publicación está pausada y no se incluye en el total.',
  BLOCKED: 'Esta publicación no está disponible y no se incluye en el total.',
  LISTING_UNAVAILABLE: 'Esta publicación ya no está disponible y no se incluye en el total.',
};

/** Carrito agrupado por vendedor (HU-15, CU-05 y RF-CAR-04). */
@Component({
  selector: 'app-cart-page',
  imports: [RouterLink],
  template: `
    <section class="cart-page" aria-labelledby="cart-title">
      <h1 id="cart-title">Tu carrito</h1>
      @if (loading()) {
        <p role="status">Cargando el carrito…</p>
      } @else if (pageError()) {
        <p role="alert">{{ pageError() }}</p>
      } @else if (cart.cart(); as current) {
        @if (!current.sellers.length) {
          <p>Tu carrito está vacío.</p>
          <a routerLink="/buscar">Explorar publicaciones</a>
        } @else {
          <div class="cart-layout">
            <div class="seller-groups">
              @for (seller of current.sellers; track seller.sellerId; let sellerIndex = $index) {
                <section class="seller-group" [attr.aria-labelledby]="'seller-' + sellerIndex">
                  <h2 [id]="'seller-' + sellerIndex">Vendedor {{ sellerIndex + 1 }}</h2>
                  @for (item of seller.items; track item.listingId) {
                    <article class="cart-item">
                      <div class="item-heading">
                        <h3>
                          <a [routerLink]="['/producto', item.listingId]">{{ item.title }}</a>
                        </h3>
                        <button
                          type="button"
                          class="remove-button"
                          [attr.aria-label]="'Quitar ' + item.title + ' del carrito'"
                          (click)="remove(item)"
                        >
                          Quitar
                        </button>
                      </div>
                      @for (warning of warningLabels(item); track warning) {
                        <p class="warning" role="status">{{ warning }}</p>
                      }
                      <div class="item-details">
                        @if (item.available) {
                          <label [for]="'quantity-' + item.listingId">Cantidad</label>
                          <input
                            type="number"
                            min="1"
                            [id]="'quantity-' + item.listingId"
                            [value]="item.quantity"
                            (change)="changeQuantity(item, $event)"
                          />
                        } @else {
                          <span>Cantidad: {{ item.quantity }}</span>
                        }
                        <span>{{ money(item.unitPrice) }} c/u</span>
                        @if (item.available) {
                          <strong>{{ money(item.subtotal) }}</strong>
                        } @else {
                          <span>Subtotal no incluido</span>
                        }
                      </div>
                      @if (quantityError()[item.listingId]; as message) {
                        <p class="stock-error" role="alert">{{ message }}</p>
                      }
                      @if (maximum()[item.listingId]; as max) {
                        <button
                          type="button"
                          class="maximum-button"
                          (click)="useMaximum(item, max)"
                        >
                          Agregar {{ max }} disponibles
                        </button>
                      }
                    </article>
                  }
                  <p class="seller-subtotal">
                    Subtotal del vendedor: <strong>{{ money(seller.subtotal) }}</strong>
                  </p>
                </section>
              }
            </div>
            <aside class="order-summary" aria-label="Resumen del carrito">
              <h2>Resumen</h2>
              <p class="total" aria-live="polite" aria-atomic="true">
                Total: <strong>{{ money(current.total) }}</strong>
              </p>
            </aside>
          </div>
        }
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
      color: #202a30;
    }
    .cart-page {
      max-width: 1050px;
      margin-inline: auto;
    }
    h1 {
      margin-top: 0;
    }
    .cart-layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 280px;
      gap: 2rem;
      align-items: start;
    }
    .seller-group,
    .order-summary {
      border: 1px solid #dce3df;
      border-radius: 14px;
      padding: 1.1rem;
      margin-bottom: 1rem;
    }
    .seller-group h2,
    .order-summary h2 {
      margin: 0 0 0.75rem;
      font-size: 1.1rem;
    }
    .cart-item {
      border-top: 1px solid #e2e6e3;
      padding-block: 1rem;
    }
    .item-heading,
    .item-details {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 0.75rem;
    }
    .item-heading h3 {
      margin: 0;
      font-size: 1rem;
    }
    a {
      color: #145d52;
    }
    button,
    input {
      min-height: 44px;
      font: inherit;
    }
    input {
      width: 5rem;
      padding: 0.4rem;
    }
    .remove-button,
    .maximum-button {
      padding: 0.45rem 0.75rem;
      border: 1px solid #145d52;
      border-radius: 8px;
      background: white;
      color: #145d52;
      cursor: pointer;
    }
    .warning {
      padding: 0.65rem;
      border-radius: 8px;
      background: #fff5da;
      color: #684b00;
    }
    .stock-error {
      color: #8f3028;
    }
    .seller-subtotal {
      text-align: right;
    }
    .order-summary {
      position: sticky;
      top: 1rem;
    }
    .total {
      font-size: 1.15rem;
    }
    @media (max-width: 720px) {
      .cart-layout {
        grid-template-columns: 1fr;
      }
      .order-summary {
        position: static;
      }
    }
  `,
})
export class CartPage {
  protected readonly cart = inject(CartService);
  protected readonly loading = signal(true);
  protected readonly pageError = signal<string | null>(null);
  protected readonly quantityError = signal<Record<string, string>>({});
  protected readonly maximum = signal<Record<string, number>>({});

  constructor() {
    this.cart.refresh().subscribe({
      next: () => this.loading.set(false),
      error: (error: unknown) => {
        this.loading.set(false);
        this.pageError.set(toProblem(error).detail);
      },
    });
  }

  protected warningLabels(item: CartItem): string[] {
    return item.warnings.map((warning) => WARNING_LABELS[warning] ?? warning);
  }

  protected money(amount: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(amount);
  }

  protected changeQuantity(item: CartItem, event: Event): void {
    const quantity = Number((event.target as HTMLInputElement).value);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity === item.quantity) return;
    this.cart.put(item.listingId, quantity).subscribe({
      next: () => this.clearError(item.listingId),
      error: (error: unknown) => this.setItemError(item.listingId, error),
    });
  }

  protected useMaximum(item: CartItem, quantity: number): void {
    this.cart.put(item.listingId, quantity).subscribe({
      next: () => this.clearError(item.listingId),
      error: (error: unknown) => this.setItemError(item.listingId, error),
    });
  }

  protected remove(item: CartItem): void {
    this.cart.remove(item.listingId).subscribe({
      error: (error: unknown) =>
        this.quantityError.update((errors) => ({
          ...errors,
          [item.listingId]: toProblem(error).detail,
        })),
    });
  }

  private setItemError(listingId: string, error: unknown): void {
    const problem = toProblem(error);
    this.quantityError.update((errors) => ({ ...errors, [listingId]: problem.detail }));
    if (problem.code === 'INSUFFICIENT_STOCK') {
      const body = error as { error?: { maximumAvailable?: unknown } };
      const max = body.error?.maximumAvailable;
      if (typeof max === 'number' && max > 0) {
        this.maximum.update((values) => ({ ...values, [listingId]: max }));
      }
    }
  }

  private clearError(listingId: string): void {
    const errors = { ...this.quantityError() };
    delete errors[listingId];
    this.quantityError.set(errors);
    const maximum = { ...this.maximum() };
    delete maximum[listingId];
    this.maximum.set(maximum);
  }
}
