import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';
import { ListingCard, ListingCardModel } from '../../shared/listing-card';

interface PublicSeller {
  storeName: string;
  activatedAt: string;
  reputation: string;
  completedSales: number;
  activeListings: number;
}
interface SellerListingsResponse {
  results: ListingCardModel[];
  total: number;
  totalPages: number;
  adjustments: string[];
}

/** Perfil público y publicaciones activas del vendedor (HU-14, RF-USR-03). */
@Component({
  selector: 'app-public-seller-page',
  imports: [ListingCard],
  template: `
    <main class="seller-page" aria-labelledby="seller-title">
      @if (loading()) {
        <p role="status">Cargando perfil…</p>
      } @else if (error()) {
        <p role="alert">{{ error() }}</p>
      } @else if (seller(); as profile) {
        <header>
          <div class="avatar" aria-hidden="true">{{ profile.storeName.charAt(0) }}</div>
          <div>
            <p class="eyebrow">Tienda</p>
            <h1 id="seller-title">{{ profile.storeName }}</h1>
            <p>{{ sellerTenure(profile.activatedAt) }}</p>
            <p>{{ profile.reputation }}</p>
          </div>
        </header>
        <section aria-label="Actividad del vendedor">
          <h2>Actividad</h2>
          <p>{{ profile.activeListings }} publicaciones activas</p>
          <p>{{ profile.completedSales }} ventas completadas</p>
        </section>
        <section aria-labelledby="listings-title">
          <h2 id="listings-title">Publicaciones activas</h2>
          @if (listingsLoading()) {
            <p role="status">Cargando publicaciones…</p>
          } @else if (listingsError()) {
            <p role="alert">{{ listingsError() }}</p>
          } @else if (listings(); as result) {
            @if (result.results.length) {
              <div class="cards">
                @for (item of result.results; track item.id) {
                  <app-listing-card [listing]="item" />
                }
              </div>
              @if (result.totalPages > 1) {
                <nav class="pagination" aria-label="Paginación de publicaciones">
                  <button type="button" [disabled]="page() === 0" (click)="changePage(page() - 1)">
                    Anterior
                  </button>
                  <span>Página {{ page() + 1 }} de {{ result.totalPages }}</span>
                  <button
                    type="button"
                    [disabled]="page() + 1 >= result.totalPages"
                    (click)="changePage(page() + 1)"
                  >
                    Siguiente
                  </button>
                </nav>
              }
            } @else {
              <p>Este vendedor no tiene publicaciones activas.</p>
            }
          }
        </section>
      }
    </main>
  `,
  styles: `
    :host {
      display: block;
    }
    .seller-page {
      max-width: 1120px;
      margin: 3rem auto;
      padding: 0 1.25rem;
    }
    header {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.5rem;
      border: 1px solid #dce3df;
      border-radius: 16px;
    }
    .avatar {
      display: grid;
      place-items: center;
      flex: 0 0 64px;
      height: 64px;
      border-radius: 50%;
      background: #dcebe4;
      color: #145d52;
      font-size: 1.5rem;
      font-weight: 700;
    }
    h1,
    .eyebrow,
    header p {
      margin: 0.2rem 0;
    }
    .eyebrow {
      color: #66716d;
    }
    section {
      margin-top: 2rem;
    }
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 230px), 1fr));
      gap: 1rem;
    }
    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 1rem;
      margin: 2rem 0;
    }
    button {
      padding: 0.65rem 0.9rem;
      border: 1px solid #145d52;
      border-radius: 8px;
      background: #145d52;
      color: white;
      font: inherit;
      cursor: pointer;
    }
    button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
  `,
})
export class PublicSellerPage {
  private readonly api = inject(ApiClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly seller = signal<PublicSeller | null>(null);
  protected readonly listings = signal<SellerListingsResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly listingsLoading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly listingsError = signal<string | null>(null);
  protected readonly page = signal(0);
  private sellerId = '';

  constructor() {
    this.sellerId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.sellerId) {
      this.loading.set(false);
      this.error.set('No se encontró el perfil del vendedor.');
      return;
    }
    this.api
      .get<PublicSeller>(`/sellers/${this.sellerId}`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (profile) => {
          this.seller.set(profile);
          this.title.setTitle(`${profile.storeName} · Vendedor en Trato`);
          this.loading.set(false);
        },
        error: (failure: unknown) => {
          this.error.set(toProblem(failure).detail);
          this.title.setTitle('Vendedor no disponible · Trato');
          this.loading.set(false);
        },
      });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const page = Math.max(0, Number(params.get('page') ?? 0) || 0);
      this.page.set(page);
      this.loadListings(page);
    });
  }

  protected sellerTenure(activatedAt: string): string {
    const date = new Date(activatedAt);
    const now = new Date();
    let years = now.getFullYear() - date.getFullYear();
    if (
      now.getMonth() < date.getMonth() ||
      (now.getMonth() === date.getMonth() && now.getDate() < date.getDate())
    )
      years--;
    if (years >= 1) return `Vendedor desde hace ${years} ${years === 1 ? 'año' : 'años'}`;
    const months = Math.max(
      0,
      (now.getFullYear() - date.getFullYear()) * 12 +
        now.getMonth() -
        date.getMonth() -
        (now.getDate() < date.getDate() ? 1 : 0),
    );
    return months < 1
      ? 'Vendedor nuevo'
      : `Vendedor desde hace ${months} ${months === 1 ? 'mes' : 'meses'}`;
  }

  protected changePage(page: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge',
    });
  }

  private loadListings(page: number): void {
    this.listingsLoading.set(true);
    this.listingsError.set(null);
    const params = new HttpParams().set('page', page);
    this.api
      .get<SellerListingsResponse>(`/sellers/${this.sellerId}/listings`, { params })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.listings.set(result);
          this.listingsLoading.set(false);
        },
        error: (failure: unknown) => {
          this.listingsError.set(toProblem(failure).detail);
          this.listingsLoading.set(false);
        },
      });
  }
}
