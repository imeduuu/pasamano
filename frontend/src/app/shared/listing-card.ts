import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface ListingCardModel {
  id: string;
  title: string;
  price: number;
  thumbnail: string | null;
  region: string;
  commune: string;
  storeName: string;
  reputation: string;
}

@Component({
  selector: 'app-listing-card',
  imports: [RouterLink],
  template: `
    <a class="card" [routerLink]="['/producto', listing().id]">
      @if (listing().thumbnail) {
        <img [src]="listing().thumbnail" [alt]="listing().title" />
      } @else {
        <div class="no-image" role="img" aria-label="Publicación sin imagen">Sin imagen</div>
      }
      <div class="card-content">
        <h2>{{ listing().title }}</h2>
        <p class="price">{{ priceLabel(listing().price) }}</p>
        <p>{{ listing().region }} · {{ listing().commune }}</p>
        <p class="seller">{{ listing().storeName }} · {{ listing().reputation }}</p>
      </div>
    </a>
  `,
  styles: `
    :host {
      display: block;
    }
    .card {
      display: block;
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
  `,
})
export class ListingCard {
  readonly listing = input.required<ListingCardModel>();

  protected priceLabel(price: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(price);
  }
}
