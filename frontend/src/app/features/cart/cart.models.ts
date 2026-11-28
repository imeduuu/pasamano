/** Vista del carrito (HU-15), agrupada por vendedor. */
export interface CartItem {
  readonly listingId: string;
  readonly title: string;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly subtotal: number;
  readonly available: boolean;
  readonly warnings: readonly string[];
}

export interface CartSeller {
  readonly sellerId: string;
  readonly items: readonly CartItem[];
  readonly subtotal: number;
}

export interface CartView {
  readonly sellers: readonly CartSeller[];
  readonly total: number;
  readonly warnings: readonly string[];
}
