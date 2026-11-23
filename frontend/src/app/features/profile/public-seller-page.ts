import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';

interface PublicSeller {
  storeName: string;
  activatedAt: string;
  reputation: string;
  completedSales: number;
  activeListings: number;
}

/** Perfil público de tienda (RF-USR-03). */
@Component({
  selector: 'app-public-seller-page',
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
            <p>Vendedor nuevo</p>
          </div>
        </header>
        <section aria-label="Actividad del vendedor">
          <h2>Actividad</h2>
          <p>{{ profile.activeListings }} publicaciones activas</p>
          <p>{{ profile.completedSales }} ventas completadas</p>
        </section>
      }
    </main>
  `,
  styles: `
    :host {
      display: block;
    }
    .seller-page {
      max-width: 800px;
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
  `,
})
export class PublicSellerPage {
  private readonly api = inject(ApiClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly seller = signal<PublicSeller | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.error.set('No se encontró el perfil del vendedor.');
      return;
    }
    this.api
      .get<PublicSeller>(`/sellers/${id}`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (seller) => {
          this.seller.set(seller);
          this.loading.set(false);
        },
        error: (failure: unknown) => {
          this.error.set(toProblem(failure).detail);
          this.loading.set(false);
        },
      });
  }
}
