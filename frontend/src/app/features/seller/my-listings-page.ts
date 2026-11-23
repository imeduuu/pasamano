import { HttpHeaders } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';

interface Listing {
  id: string;
  title: string;
  price: number;
  stock: number;
  status: string;
  version: number;
  images: { url: string; thumbnailUrl: string }[];
}
interface ListingPage {
  items: Listing[];
  page: number;
  size: number;
  hasNext: boolean;
}

const PAGE_SIZE = 20;
const STATUS_LABELS: Record<string, string> = {
  BORRADOR: 'Borrador',
  ACTIVA: 'Activa',
  PAUSADA: 'Pausada',
  AGOTADA: 'Agotada',
  BLOQUEADA: 'Bloqueada',
};
const STATUS_CLASSES: Record<string, string> = {
  BORRADOR: 'status-draft',
  ACTIVA: 'status-active',
  PAUSADA: 'status-paused',
  AGOTADA: 'status-empty',
  BLOQUEADA: 'status-blocked',
};

/** Gestión paginada de publicaciones propias (RF-PUB-08). */
@Component({
  selector: 'app-my-listings-page',
  imports: [RouterLink],
  template: `
    <main class="listings-page" aria-labelledby="listings-title">
      <header class="listings-header">
        <div>
          <h1 id="listings-title">Mis publicaciones</h1>
          <p>Administra tus productos publicados.</p>
        </div>
        <a class="button" routerLink="/publicar-producto">Publicar producto</a>
      </header>
      <div class="listings-filter">
        <label for="status-filter">Filtrar por estado</label>
        <select id="status-filter" [value]="status()" (change)="filter($event)">
          <option value="">Todos los estados</option>
          <option value="BORRADOR">Borrador</option>
          <option value="ACTIVA">Activa</option>
          <option value="PAUSADA">Pausada</option>
          <option value="AGOTADA">Agotada</option>
          <option value="BLOQUEADA">Bloqueada</option>
        </select>
      </div>
      @if (error()) {
        <p class="field-error" role="alert">{{ error() }}</p>
      }
      @if (loading()) {
        <p role="status">Cargando publicaciones…</p>
      } @else if (items().length === 0) {
        <section class="empty-listings">
          <p>Aún no tienes publicaciones en este estado.</p>
          <a routerLink="/publicar-producto">Publicar un producto</a>
        </section>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Producto</th>
                <th scope="col">Precio</th>
                <th scope="col">Stock</th>
                <th scope="col">Estado</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (listing of items(); track listing.id) {
                <tr [attr.data-listing-id]="listing.id">
                  <td class="product-cell">
                    @if (listing.images[0]; as image) {
                      <img [src]="image.thumbnailUrl" [alt]="'Miniatura de ' + listing.title" />
                    } @else {
                      <span class="image-placeholder" aria-hidden="true">Sin imagen</span>
                    }
                    <span>{{ listing.title }}</span>
                  </td>
                  <td>{{ priceLabel(listing.price) }}</td>
                  <td>{{ listing.stock }}</td>
                  <td>
                    <span
                      class="status"
                      [class]="statusClass(listing.status)"
                      [attr.data-status]="listing.status"
                      >{{ statusLabel(listing.status) }}</span
                    >
                  </td>
                  <td class="actions">
                    <a
                      [routerLink]="['/publicar-producto', listing.id]"
                      [attr.aria-label]="'Editar ' + listing.title"
                      >Editar</a
                    >
                    @if (listing.status === 'BORRADOR') {
                      <button
                        type="button"
                        (click)="transition(listing, 'publish')"
                        [attr.aria-label]="'Publicar ' + listing.title"
                      >
                        Publicar
                      </button>
                    }
                    @if (listing.status === 'ACTIVA') {
                      <button
                        type="button"
                        (click)="transition(listing, 'pause')"
                        [attr.aria-label]="'Pausar ' + listing.title"
                      >
                        Pausar
                      </button>
                    }
                    @if (listing.status === 'PAUSADA' || listing.status === 'AGOTADA') {
                      <button
                        type="button"
                        (click)="transition(listing, 'activate')"
                        [attr.aria-label]="'Reactivar ' + listing.title"
                      >
                        Reactivar
                      </button>
                    }
                    @if (listing.status !== 'BLOQUEADA') {
                      <button
                        type="button"
                        class="delete-action"
                        (click)="askDelete(listing)"
                        [attr.aria-label]="'Eliminar ' + listing.title"
                      >
                        Eliminar
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <nav class="pagination" aria-label="Paginación de publicaciones">
          <button
            type="button"
            [disabled]="page() === 0 || loading()"
            aria-label="Página anterior"
            (click)="load(page() - 1)"
          >
            Anterior
          </button>
          <span>Página {{ page() + 1 }}</span>
          <button
            type="button"
            [disabled]="!hasNext() || loading()"
            aria-label="Página siguiente"
            (click)="load(page() + 1)"
          >
            Siguiente
          </button>
        </nav>
      }
      @if (pendingDelete(); as listing) {
        <div class="dialog-backdrop">
          <section
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-title"
            aria-describedby="delete-description"
            class="confirm-dialog"
          >
            <h2 id="delete-title">¿Eliminar publicación?</h2>
            <p id="delete-description">
              ¿Eliminar “{{ listing.title }}”? Esta acción no se puede deshacer.
            </p>
            <div class="dialog-actions">
              <button type="button" (click)="pendingDelete.set(null)">Cancelar</button
              ><button type="button" data-confirm (click)="delete(listing)">Eliminar</button>
            </div>
          </section>
        </div>
      }
    </main>
  `,
  styles: `
    :host {
      display: block;
    }
    .listings-page {
      max-width: 1100px;
      margin: 2rem auto;
      padding: 0 1rem;
    }
    .listings-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    h1 {
      margin-bottom: 0.25rem;
    }
    .listings-header p {
      margin-top: 0;
      color: #58636c;
    }
    .listings-filter {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    select,
    button {
      font: inherit;
      padding: 0.55rem 0.75rem;
    }
    button,
    a {
      cursor: pointer;
    }
    .table-wrap {
      overflow-x: auto;
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      text-align: left;
      padding: 0.8rem;
      border-bottom: 1px solid #d9dee2;
      vertical-align: middle;
    }
    .product-cell {
      min-width: 220px;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .product-cell img,
    .image-placeholder {
      width: 56px;
      height: 56px;
      object-fit: cover;
      border-radius: 6px;
      background: #eef1f3;
    }
    .image-placeholder {
      display: grid;
      place-items: center;
      font-size: 0.65rem;
      text-align: center;
    }
    .status {
      display: inline-block;
      border-radius: 999px;
      padding: 0.25rem 0.6rem;
      font-size: 0.85rem;
      font-weight: 600;
    }
    .status-active {
      color: #14532d;
      background: #dcfce7;
    }
    .status-draft {
      color: #334155;
      background: #e2e8f0;
    }
    .status-paused {
      color: #854d0e;
      background: #fef3c7;
    }
    .status-empty {
      color: #7c2d12;
      background: #ffedd5;
    }
    .status-blocked {
      color: #991b1b;
      background: #fee2e2;
    }
    .actions {
      min-width: 230px;
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    .actions button,
    .actions a {
      border: 0;
      background: none;
      color: #075ea8;
      padding: 0.35rem;
      font: inherit;
    }
    .actions .delete-action {
      color: #a12622;
    }
    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 1rem;
      padding: 1.25rem;
    }
    .pagination button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .empty-listings {
      padding: 2rem;
      text-align: center;
      background: #f5f7f8;
      border-radius: 8px;
    }
    .dialog-backdrop {
      position: fixed;
      inset: 0;
      display: grid;
      place-items: center;
      padding: 1rem;
      background: #0008;
      z-index: 1000;
    }
    .confirm-dialog {
      width: min(100%, 28rem);
      padding: 1.5rem;
      border-radius: 10px;
      background: white;
      box-shadow: 0 12px 40px #0004;
    }
    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }
    @media (max-width: 650px) {
      .listings-header {
        align-items: flex-start;
        flex-direction: column;
      }
      .listings-filter {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  `,
})
export class MyListingsPage {
  private readonly api = inject(ApiClient);
  protected readonly items = signal<Listing[]>([]);
  protected readonly page = signal(0);
  protected readonly hasNext = signal(false);
  protected readonly status = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly pendingDelete = signal<Listing | null>(null);

  constructor() {
    this.load(0);
  }

  protected statusLabel(status: string): string {
    return STATUS_LABELS[status] ?? status;
  }
  protected statusClass(status: string): string {
    return `status ${STATUS_CLASSES[status] ?? 'status-draft'}`;
  }
  protected priceLabel(price: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(price);
  }

  protected filter(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value);
    this.load(0);
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    const params: Record<string, string | number> = { page, size: PAGE_SIZE };
    if (this.status()) params['status'] = this.status();
    this.api.get<ListingPage>('/me/listings', { params }).subscribe({
      next: (result) => {
        this.items.set(result.items);
        this.page.set(result.page);
        this.hasNext.set(result.hasNext);
        this.loading.set(false);
      },
      error: (failure: unknown) => {
        this.error.set(toProblem(failure).detail);
        this.loading.set(false);
      },
    });
  }

  protected transition(listing: Listing, action: 'publish' | 'pause' | 'activate'): void {
    this.api
      .postResponse<Listing>(
        `/listings/${listing.id}/${action}`,
        {},
        {
          headers: new HttpHeaders({ 'If-Match': `"${listing.version}"` }),
        },
      )
      .subscribe({
        next: () => this.load(this.page()),
        error: (failure: unknown) => this.error.set(toProblem(failure).detail),
      });
  }

  protected askDelete(listing: Listing): void {
    this.pendingDelete.set(listing);
  }

  protected delete(listing: Listing): void {
    this.api
      .delete<void>(`/listings/${listing.id}`, {
        headers: new HttpHeaders({ 'If-Match': `"${listing.version}"` }),
      })
      .subscribe({
        next: () => {
          this.pendingDelete.set(null);
          this.load(this.page());
        },
        error: (failure: unknown) => {
          this.pendingDelete.set(null);
          this.error.set(toProblem(failure).detail);
        },
      });
  }
}
