import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';

interface ListingImage {
  url: string;
  thumbnailUrl: string;
}
interface Listing {
  id: string;
  title: string;
  description: string;
  price: number;
  stock: number;
  condition: string;
  region: string;
  commune: string;
  status: string;
  sellerId: string;
  storeName: string;
  images: ListingImage[];
}
interface Region {
  code: string;
  name: string;
  communes: { code: string; name: string }[];
}

const CONDITIONS: Record<string, string> = {
  NUEVO: 'Nuevo',
  COMO_NUEVO: 'Como nuevo',
  USADO: 'Usado',
  PARA_REPARAR: 'Para reparar',
};

/** Detalle público de una publicación y su vendedor (RF-BUS-05). */
@Component({
  selector: 'app-listing-detail-page',
  imports: [RouterLink],
  template: `
    <main class="detail-page" aria-labelledby="listing-title">
      @if (loading()) {
        <p role="status">Cargando publicación…</p>
      } @else if (error()) {
        <p role="alert">{{ error() }}</p>
      } @else if (listing(); as item) {
        <a class="back-link" routerLink="/">Volver a publicaciones</a>
        <div class="detail-layout">
          <section class="gallery" aria-label="Galería del producto">
            @if (item.images.length) {
              <div class="main-image-wrap">
                <img
                  data-main-image
                  class="main-image"
                  [src]="item.images[selectedImage()].url"
                  [alt]="item.title"
                />
                <span class="image-count"
                  >{{ selectedImage() + 1 }} / {{ item.images.length }}</span
                >
              </div>
              @if (item.images.length > 1) {
                <div class="thumbnails" role="group" aria-label="Miniaturas del producto">
                  @for (image of item.images; track image.thumbnailUrl; let i = $index) {
                    <button
                      type="button"
                      data-thumbnail
                      [class.selected]="i === selectedImage()"
                      [attr.aria-label]="'Ver imagen ' + (i + 1) + ' de ' + item.images.length"
                      [attr.aria-pressed]="i === selectedImage()"
                      (click)="selectImage(i)"
                      (keydown)="navigateImages($event, i, item.images.length)"
                    >
                      <img [src]="image.thumbnailUrl" [alt]="''" />
                    </button>
                  }
                </div>
              }
            } @else {
              <div class="no-image" role="img" aria-label="Esta publicación no tiene imágenes">
                Sin imágenes
              </div>
            }
          </section>
          <section class="product-info">
            <p class="condition">{{ conditionLabel(item.condition) }}</p>
            <h1 id="listing-title">{{ item.title }}</h1>
            @if (unavailable(item)) {
              <p class="unavailable" role="status">No disponible</p>
            } @else {
              <p class="price">{{ priceLabel(item.price) }}</p>
              <p class="stock">{{ item.stock }} disponibles</p>
            }
            <dl class="details">
              <div>
                <dt>Condición</dt>
                <dd>{{ conditionLabel(item.condition) }}</dd>
              </div>
              <div>
                <dt>Ubicación</dt>
                <dd>{{ regionName() }} · {{ communeName() }}</dd>
              </div>
            </dl>
            <p class="description">{{ item.description }}</p>
            <aside class="seller-card" aria-label="Información del vendedor">
              <div class="seller-avatar" aria-hidden="true">{{ item.storeName.charAt(0) }}</div>
              <div>
                <p class="seller-label">Vendido por</p>
                <strong>{{ item.storeName }}</strong>
                <p class="seller-reputation">Vendedor nuevo</p>
              </div>
              <a data-seller-profile [routerLink]="['/vendedores', item.sellerId]">Ver perfil</a>
            </aside>
          </section>
        </div>
      }
    </main>
  `,
  styles: `
    :host {
      display: block;
      color: #202a30;
    }
    .detail-page {
      max-width: 1120px;
      margin: 2rem auto;
      padding: 0 1.25rem 3rem;
    }
    .back-link {
      display: inline-block;
      margin-bottom: 1.25rem;
      color: #145d52;
    }
    .detail-layout {
      display: grid;
      grid-template-columns: minmax(0, 1.1fr) minmax(300px, 0.9fr);
      gap: clamp(2rem, 5vw, 4rem);
    }
    .gallery {
      min-width: 0;
    }
    .main-image-wrap {
      position: relative;
      display: grid;
      place-items: center;
      aspect-ratio: 1 / 0.82;
      background: #f4f5f2;
      border-radius: 18px;
      overflow: hidden;
    }
    .main-image {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
    .image-count {
      position: absolute;
      right: 1rem;
      bottom: 1rem;
      padding: 0.35rem 0.65rem;
      border-radius: 999px;
      background: #172522d9;
      color: white;
      font-size: 0.85rem;
    }
    .thumbnails {
      display: flex;
      gap: 0.75rem;
      margin-top: 0.85rem;
      overflow-x: auto;
    }
    .thumbnails button {
      width: 76px;
      height: 76px;
      flex: 0 0 auto;
      padding: 3px;
      border: 2px solid transparent;
      border-radius: 10px;
      background: #f4f5f2;
      cursor: pointer;
    }
    .thumbnails button.selected,
    .thumbnails button:focus-visible {
      border-color: #145d52;
      outline: 2px solid #145d52;
      outline-offset: 2px;
    }
    .thumbnails img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 6px;
    }
    .no-image {
      display: grid;
      place-items: center;
      min-height: 360px;
      border-radius: 18px;
      background: #f4f5f2;
      color: #66716d;
    }
    .condition {
      color: #56635e;
      margin: 0 0 0.5rem;
    }
    h1 {
      margin: 0 0 0.75rem;
      font-size: clamp(1.8rem, 4vw, 2.5rem);
      line-height: 1.15;
    }
    .price {
      margin: 0.8rem 0 0.25rem;
      font-size: 2rem;
      font-weight: 700;
    }
    .stock {
      color: #4d5c55;
      margin-top: 0.25rem;
    }
    .unavailable {
      display: inline-block;
      padding: 0.55rem 0.8rem;
      border-radius: 8px;
      background: #fce9e6;
      color: #8f3028;
      font-weight: 700;
    }
    .details {
      display: grid;
      gap: 0.8rem;
      margin: 1.5rem 0;
    }
    .details div {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      border-bottom: 1px solid #e2e6e3;
      padding-bottom: 0.7rem;
    }
    dt {
      color: #66716d;
    }
    dd {
      margin: 0;
      font-weight: 600;
      text-align: right;
    }
    .description {
      white-space: pre-line;
      line-height: 1.6;
    }
    .seller-card {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      margin-top: 2rem;
      padding: 1rem;
      border: 1px solid #dce3df;
      border-radius: 14px;
    }
    .seller-avatar {
      display: grid;
      place-items: center;
      flex: 0 0 46px;
      height: 46px;
      border-radius: 50%;
      background: #dcebe4;
      color: #145d52;
      font-size: 1.2rem;
      font-weight: 700;
    }
    .seller-label,
    .seller-reputation {
      margin: 0;
      color: #66716d;
      font-size: 0.85rem;
    }
    .seller-card strong {
      display: block;
      margin: 0.15rem 0;
    }
    .seller-card a {
      margin-left: auto;
      color: #145d52;
      font-weight: 600;
      white-space: nowrap;
    }
    @media (max-width: 760px) {
      .detail-layout {
        grid-template-columns: 1fr;
        gap: 1.5rem;
      }
      .no-image {
        min-height: 240px;
      }
    }
  `,
})
export class ListingDetailPage {
  private readonly api = inject(ApiClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  protected readonly listing = signal<Listing | null>(null);
  protected readonly regions = signal<Region[]>([]);
  protected readonly selectedImage = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('No se encontró la publicación.');
      this.loading.set(false);
      return;
    }
    forkJoin({
      listing: this.api.get<Listing>(`/listings/${id}`),
      regions: this.api.get<Region[]>('/regions'),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ listing, regions }) => {
          this.listing.set(listing);
          this.regions.set(regions);
          this.title.setTitle(`${listing.title} · Trato`);
          this.meta.updateTag({
            name: 'description',
            content: `${listing.title}: ${this.conditionLabel(listing.condition)} en ${this.locationLabel(listing, regions)}. ${this.priceLabel(listing.price)} en Trato.`,
          });
          this.loading.set(false);
        },
        error: (failure: unknown) => {
          this.error.set(toProblem(failure).detail);
          this.title.setTitle('Publicación no disponible · Trato');
          this.meta.updateTag({
            name: 'description',
            content: 'Consulta productos publicados en Trato.',
          });
          this.loading.set(false);
        },
      });
  }

  protected conditionLabel(condition: string): string {
    return CONDITIONS[condition] ?? condition;
  }

  protected priceLabel(price: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(price);
  }

  protected unavailable(listing: Listing): boolean {
    return listing.status !== 'ACTIVA' || listing.stock <= 0;
  }

  protected regionName(): string {
    const listing = this.listing();
    return (
      this.regions().find((region) => region.code === listing?.region)?.name ??
      listing?.region ??
      ''
    );
  }

  protected communeName(): string {
    const listing = this.listing();
    const region = this.regions().find((item) => item.code === listing?.region);
    return (
      region?.communes.find((commune) => commune.code === listing?.commune)?.name ??
      listing?.commune ??
      ''
    );
  }

  protected selectImage(index: number): void {
    this.selectedImage.set(index);
  }

  protected navigateImages(event: KeyboardEvent, index: number, count: number): void {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    const next = (index + (event.key === 'ArrowRight' ? 1 : -1) + count) % count;
    this.selectImage(next);
    const thumbnails = (
      event.currentTarget as HTMLElement
    ).parentElement?.querySelectorAll<HTMLButtonElement>('[data-thumbnail]');
    thumbnails?.[next]?.focus();
  }

  private locationLabel(listing: Listing, regions: Region[]): string {
    const region = regions.find((item) => item.code === listing.region);
    const commune = region?.communes.find((item) => item.code === listing.commune);
    return [region?.name ?? listing.region, commune?.name ?? listing.commune].join(', ');
  }
}
