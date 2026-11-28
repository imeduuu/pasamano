import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';
import { ListingCard, ListingCardModel } from '../../shared/listing-card';

type SearchListing = ListingCardModel;

interface SearchResponse {
  results: SearchListing[];
  total: number;
  totalPages: number;
  adjustments: string[];
}
interface Category {
  id: string;
  name: string;
  children: Category[];
}
interface Region {
  code: string;
  name: string;
  communes: { code: string; name: string }[];
}

/** BÃºsqueda compartible del catÃ¡logo (HU-12, CU-04). */
@Component({
  selector: 'app-search-page',
  imports: [ListingCard],
  template: `
    <section class="search-page" aria-labelledby="search-title">
      <h1 #heading id="search-title" tabindex="-1">Buscar publicaciones</h1>
      <form role="search" aria-label="Buscar publicaciones" (submit)="submit($event, field.value)">
        <label for="search-query">Â¿QuÃ© estÃ¡s buscando?</label>
        <div class="search-form">
          <input #field id="search-query" type="search" name="q" [value]="query()" />
          <button type="submit">Buscar</button>
        </div>
      </form>

      <button
        class="filter-toggle"
        data-filter-toggle
        type="button"
        aria-controls="search-filters"
        [attr.aria-expanded]="filtersOpen()"
        (click)="filtersOpen.update((v) => !v)"
      >
        Filtros <span data-active-filter-count>{{ activeFilterCount() }}</span>
      </button>
      <div class="search-layout">
        <aside
          id="search-filters"
          class="filters"
          [class.filters-open]="filtersOpen()"
          aria-label="Filtros de bÃºsqueda"
        >
          <fieldset>
            <legend>CategorÃ­a</legend>
            <label for="category-parent">CategorÃ­a principal</label>
            <select
              id="category-parent"
              [value]="parentCategory()"
              (change)="selectParent($any($event.target).value)"
            >
              <option value="">Todas</option>
              @for (item of categories(); track item.id) {
                <option [value]="item.id">{{ item.name }}</option>
              }
            </select>
            <label for="category">SubcategorÃ­a</label>
            <select
              id="category"
              [disabled]="!parentCategory()"
              [value]="filters()['category'] ?? ''"
              (change)="setFilter('category', $any($event.target).value)"
            >
              <option value="">Todas</option>
              @for (item of childCategories(); track item.id) {
                <option [value]="item.id">{{ item.name }}</option>
              }
            </select>
          </fieldset>
          <fieldset>
            <legend>Precio (CLP)</legend>
            <label for="minPrice">MÃ­nimo</label
            ><input
              id="minPrice"
              type="number"
              min="0"
              [value]="filters()['minPrice'] ?? ''"
              (change)="setFilter('minPrice', $any($event.target).value)"
            />
            <label for="maxPrice">MÃ¡ximo</label
            ><input
              id="maxPrice"
              type="number"
              min="0"
              [value]="filters()['maxPrice'] ?? ''"
              (change)="setFilter('maxPrice', $any($event.target).value)"
            />
          </fieldset>
          <label for="condition">CondiciÃ³n</label>
          <select
            id="condition"
            [value]="filters()['condition'] ?? ''"
            (change)="setFilter('condition', $any($event.target).value)"
          >
            <option value="">Todas</option>
            <option value="NUEVO">Nuevo</option>
            <option value="COMO_NUEVO">Como nuevo</option>
            <option value="USADO">Usado</option>
            <option value="PARA_REPARAR">Para reparar</option>
          </select>
          <label for="region">RegiÃ³n</label>
          <select
            id="region"
            [value]="filters()['region'] ?? ''"
            (change)="setFilter('region', $any($event.target).value)"
          >
            <option value="">Todas</option>
            @for (item of regions(); track item.code) {
              <option [value]="item.code">{{ item.name }}</option>
            }
          </select>
          <label for="minRating">ReputaciÃ³n mÃ­nima</label>
          <select
            id="minRating"
            [value]="filters()['minRating'] ?? ''"
            (change)="setFilter('minRating', $any($event.target).value)"
          >
            <option value="">Cualquiera</option>
            <option value="3">3 estrellas</option>
            <option value="4">4 estrellas</option>
            <option value="4.5">4,5 estrellas</option>
          </select>
          <button type="button" class="clear-filters" (click)="clearFilters()">
            Limpiar filtros
          </button>
        </aside>
        <div class="results">
          <label class="sort-label" for="sort">Ordenar por</label>
          <select
            id="sort"
            [value]="filters()['sort'] ?? defaultSort()"
            (change)="setFilter('sort', $any($event.target).value)"
          >
            <option value="relevance">Relevancia</option>
            <option value="price_asc">Precio: menor a mayor</option>
            <option value="recent">MÃ¡s recientes</option>
          </select>

          @if (loading()) {
            <p class="state" role="status">Buscando publicacionesâ€¦</p>
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
                  <app-listing-card [listing]="item" />
                }
              </div>
              @if (data.totalPages > 1) {
                <nav class="pagination" aria-label="PaginaciÃ³n de resultados">
                  <button type="button" [disabled]="page() === 0" (click)="changePage(page() - 1)">
                    Anterior
                  </button>
                  <span>PÃ¡gina {{ page() + 1 }} de {{ data.totalPages }}</span>
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
                <p>Quita algunos filtros para ampliar tu bÃºsqueda.</p>
                <p>Revisa la ortografÃ­a o prueba con otras palabras.</p>
              </div>
            }
          }
        </div>
      </div>
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
    .filter-toggle {
      display: none;
      margin-top: 1rem;
    }
    .search-layout {
      display: grid;
      grid-template-columns: 230px minmax(0, 1fr);
      gap: 1.5rem;
      margin-top: 1rem;
    }
    .filters {
      align-self: start;
      display: grid;
      gap: 0.65rem;
      padding: 1rem;
      border: 1px solid #dce3df;
      border-radius: 12px;
    }
    .filters fieldset {
      display: grid;
      gap: 0.5rem;
      padding: 0;
      border: 0;
    }
    .filters legend {
      font-weight: 700;
    }
    .filters label,
    .sort-label {
      font-weight: 600;
    }
    .filters select,
    .filters input,
    .results > select {
      width: 100%;
      min-width: 0;
      padding: 0.55rem;
      border: 1px solid #aab5af;
      border-radius: 8px;
      font: inherit;
    }
    .clear-filters {
      margin-top: 0.4rem;
    }
    .results {
      min-width: 0;
    }
    .sort-label {
      margin-right: 0.5rem;
    }
    .results > select {
      width: auto;
      margin-bottom: 1rem;
    }
    @media (max-width: 600px) {
      .filter-toggle {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
      }
      .search-layout {
        display: block;
      }
      .filters {
        display: none;
        margin-bottom: 1rem;
      }
      .filters.filters-open {
        display: grid;
      }
      .sort-label {
        display: block;
        margin-bottom: 0.35rem;
      }
      .results > select {
        width: 100%;
      }
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
  protected readonly filters = signal<Record<string, string>>({});
  protected readonly categories = signal<Category[]>([]);
  protected readonly regions = signal<Region[]>([]);
  protected readonly parentCategory = signal('');
  protected readonly filtersOpen = signal(false);
  protected readonly page = signal(0);
  protected readonly response = signal<SearchResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private hasLoaded = false;
  private lastPage = 0;

  protected defaultSort(): string {
    return this.query() ? 'relevance' : 'recent';
  }
  protected childCategories(): Category[] {
    return this.categories().find((item) => item.id === this.parentCategory())?.children ?? [];
  }
  protected activeFilterCount(): number {
    const f = this.filters();
    return ['category', 'minPrice', 'maxPrice', 'condition', 'region', 'minRating'].filter(
      (key) => !!f[key],
    ).length;
  }

  constructor() {
    this.api
      .get<Category[]>('/categories')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.categories.set(items);
          const id = this.filters()['category'];
          this.parentCategory.set(
            items.find((item) => item.id === id || item.children.some((child) => child.id === id))
              ?.id ?? '',
          );
        },
        error: () => this.categories.set([]),
      });
    this.api
      .get<Region[]>('/regions')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (items) => this.regions.set(items), error: () => this.regions.set([]) });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const query = params.get('q') ?? '';
      const page = Math.max(0, Number(params.get('page') ?? 0) || 0);
      const pageChanged = this.hasLoaded && page !== this.lastPage;
      this.query.set(query);
      const filters: Record<string, string> = {};
      for (const key of [
        'category',
        'minPrice',
        'maxPrice',
        'condition',
        'region',
        'minRating',
        'sort',
      ]) {
        const value = params.get(key);
        if (value) filters[key] = value;
      }
      this.filters.set(filters);
      this.parentCategory.set(
        this.categories().find((item) =>
          item.children.some((child) => child.id === filters['category']),
        )?.id ?? '',
      );
      this.page.set(page);
      this.lastPage = page;
      this.loading.set(true);
      this.error.set(null);
      let requestParams = new HttpParams().set('page', page);
      if (query) requestParams = requestParams.set('q', query);
      for (const [key, value] of Object.entries(filters))
        requestParams = requestParams.set(key, value);
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

  protected selectParent(value: string): void {
    this.parentCategory.set(value);
    this.setFilter('category', value);
  }

  protected setFilter(key: string, value: string): void {
    const f = { ...this.filters(), [key]: value };
    const categoryParent = this.parentCategory();
    if (key === 'category' && value && !categoryParent) {
      const parent = this.categories().find((item) =>
        item.children.some((child) => child.id === value),
      );
      this.parentCategory.set(parent?.id ?? '');
    }
    if (!value) delete f[key];
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...f, page: null },
      queryParamsHandling: 'merge',
    });
  }

  private categoryParent(categoryId?: string): string {
    if (!categoryId) return '';
    return (
      this.categories().find(
        (item) => item.id === categoryId || item.children.some((child) => child.id === categoryId),
      )?.id ?? ''
    );
  }

  protected clearFilters(): void {
    this.parentCategory.set('');
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        category: null,
        minPrice: null,
        maxPrice: null,
        condition: null,
        region: null,
        minRating: null,
        sort: null,
        page: null,
      },
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
    const labels: Record<string, string> = {
      PRICE_RANGE_RESET: 'el precio mÃ­nimo era mayor que el mÃ¡ximo y se quitaron ambos lÃ­mites',
      ORDER_DEFAULTED: 'se aplicÃ³ el orden predeterminado',
      PAGE_SIZE_DEFAULTED: 'se aplicÃ³ el tamaÃ±o de pÃ¡gina predeterminado',
      PAGE_SIZE_RESET: 'se restableciÃ³ el tamaÃ±o de pÃ¡gina',
    };
    return `Ajustamos algunos criterios de bÃºsqueda: ${adjustments.map((item) => labels[item] ?? item).join('; ')}.`;
  }
}
