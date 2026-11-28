import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';

interface SearchListing {
  id: string;
  title: string;
  price: number;
  thumbnail: string | null;
  region: string;
  commune: string;
  storeName: string;
  reputation: string;
}

interface SearchResponse {
  results: SearchListing[];
  total: number;
  totalPages: number;
  adjustments: string[];
}

/** Búsqueda compartible del catálogo (HU-12, CU-04). */
@Component({
  selector: 'app-search-page',
  template: `
    <section class="search-page" aria-labelledby="search-title">
      <h1 #heading id="search-title" tabindex="-1">Buscar publicaciones</h1>
      <form role="search" aria-label="Buscar publicaciones" (submit)="submit($event, field.value)">
        <label for="search-query">¿Qué estás buscando?</label>
        <div class="search-form">
          <input #field id="search-query" type="search" name="q" [value]="query()" />
          <button type="submit">Buscar</button>
        </div>
      </form>

      @if (loading()) {
        <p class="state" role="status">Buscando publicaciones…</p>
      } @else if (error()) {
        <p class="state error" role="alert">{{ error() }}</p>
      } @else if (response(); as data) {
        <p class="result-count" aria-live="polite" aria-atomic="true">
          {{ data.total }} {{ data.total === 1 ? 'resultado' : 'resultados' }}
        </p>
        @if (data.adjustments.length) {
          <p role="status">{{ adjustmentMessage(data.adjustments) }}</p>
        }
        @if (data.results.length) {
          <div class="cards">
            @for (item of data.results; track item.id) {
              <a class="card" [href]="'/producto/' + item.id">
                @if (item.thumbnail) {
                  <img [src]="item.thumbnail" [alt]="item.title" />
                } @else {
                  <div class="no-image" role="img" aria-label="Publicación sin imagen">
                    Sin imagen
                  </div>
                }
                <div class="card-content">
                  <h2>{{ item.title }}</h2>
                  <p class="price">{{ priceLabel(item.price) }}</p>
                  <p>{{ item.region }} · {{ item.commune }}</p>
                  <p class="seller">{{ item.storeName }} · {{ item.reputation }}</p>
                </div>
              </a>
            }
          </div>
          @if (data.totalPages > 1) {
            <nav class="pagination" aria-label="Paginación de resultados">
              <button type="button" [disabled]="page() === 0" (click)="changePage(page() - 1)">
                Anterior
              </button>
              <span>Página {{ page() + 1 }} de {{ data.totalPages }}</span>
              <button
                data-next-page
                type="button"
                [disabled]="page() + 1 >= data.totalPages"
                (click)="changePage(page() + 1)"
              >
                Siguiente
              </button>
            </nav>
          }
        } @else {
          <div class="empty-state">
            <h2>No encontramos publicaciones</h2>
            <p>Quita algunos filtros para ampliar tu búsqueda.</p>
            <p>Revisa la ortografía o prueba con otras palabras.</p>
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
    .search-page {
      max-width: 1120px;
      margin: 0 auto;
    }
    h1 {
      font-size: clamp(1.7rem, 4vw, 2.3rem);
    }
    h1:focus {
      outline: 3px solid #145d52;
      outline-offset: 4px;
    }
    form {
      max-width: 38rem;
    }
    form > label {
      display: block;
      margin-bottom: 0.4rem;
      font-weight: 600;
    }
    .search-form {
      display: flex;
      gap: 0.5rem;
    }
    input {
      min-width: 0;
      flex: 1;
      padding: 0.7rem;
      border: 1px solid #aab5af;
      border-radius: 8px;
      font: inherit;
    }
    button {
      padding: 0.65rem 0.9rem;
      border: 1px solid #145d52;
      border-radius: 8px;
      background: #145d52;
      color: white;
      font: inherit;
      font-weight: 600;
      cursor: pointer;
    }
    button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
    .result-count {
      margin-block: 1.5rem;
      font-weight: 700;
    }
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 230px), 1fr));
      gap: 1rem;
    }
    .card {
      overflow: hidden;
      color: inherit;
      text-decoration: none;
      border: 1px solid #dce3df;
      border-radius: 12px;
      background: white;
    }
    .card:focus-visible {
      outline: 3px solid #145d52;
      outline-offset: 2px;
    }
    .card > img,
    .no-image {
      display: grid;
      place-items: center;
      width: 100%;
      height: 190px;
      object-fit: cover;
      background: #f2f4f1;
    }
    .no-image {
      color: #66716d;
    }
    .card-content {
      padding: 0.9rem;
    }
    h2 {
      margin: 0;
      font-size: 1.05rem;
    }
    .card p {
      margin: 0.4rem 0 0;
    }
    .price {
      font-size: 1.2rem;
      font-weight: 700;
    }
    .seller {
      color: #56635e;
      font-size: 0.9rem;
    }
    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 1rem;
      margin: 2rem 0;
    }
    .state,
    .empty-state {
      margin-block: 2rem;
    }
    .error {
      color: #8f3028;
    }
  `,
})
export class SearchPage {
  private readonly api = inject(ApiClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('heading') private heading?: ElementRef<HTMLHeadingElement>;
  protected readonly query = signal('');
  protected readonly page = signal(0);
  protected readonly response = signal<SearchResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private hasLoaded = false;
  private lastPage = 0;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const query = params.get('q') ?? '';
      const page = Math.max(0, Number(params.get('page') ?? 0) || 0);
      const pageChanged = this.hasLoaded && page !== this.lastPage;
      this.query.set(query);
      this.page.set(page);
      this.lastPage = page;
      this.loading.set(true);
      this.error.set(null);
      let requestParams = new HttpParams().set('page', page);
      if (query) requestParams = requestParams.set('q', query);
      this.api
        .get<SearchResponse>('/listings', { params: requestParams })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (result) => {
            this.response.set(result);
            this.loading.set(false);
            if (pageChanged) setTimeout(() => this.heading?.nativeElement.focus());
            this.hasLoaded = true;
          },
          error: (failure: unknown) => {
            this.error.set(toProblem(failure).detail);
            this.loading.set(false);
            this.response.set(null);
            this.hasLoaded = true;
          },
        });
    });
  }

  protected submit(event: Event, value: string): void {
    event.preventDefault();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { q: value.trim() || null, page: null },
    });
  }

  protected changePage(page: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge',
    });
  }

  protected priceLabel(price: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(price);
  }

  protected adjustmentMessage(adjustments: string[]): string {
    return `Ajustamos algunos criterios de búsqueda: ${adjustments.join(', ')}.`;
  }
}
