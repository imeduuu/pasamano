import { HttpHeaders } from '@angular/common/http';
import { Component, inject, signal, viewChild } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiClient } from '../../core/http/api-client';
import { toProblem } from '../../core/http/problem';
import { ListingImageUpload } from './listing-image-upload';

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
interface Listing {
  id: string;
  title: string;
  description: string;
  price: number;
  stock: number;
  condition: string;
  categoryId: string;
  region: string;
  commune: string;
  status: string;
}

const wholeNumber: ValidatorFn = (control): ValidationErrors | null => {
  const value = control.value;
  if (value === '' || value === null || value === undefined) return null;
  return /^\d+$/.test(String(value).replaceAll('.', '')) ? null : { wholeNumber: true };
};
const priceRange: ValidatorFn = (control): ValidationErrors | null => {
  if (!control.value) return null;
  const amount = Number(String(control.value).replaceAll('.', ''));
  return amount < 100
    ? { min: { min: 100, actual: amount } }
    : amount > 99999999
      ? { max: { max: 99999999, actual: amount } }
      : null;
};
const titleValidator: ValidatorFn = (control): ValidationErrors | null => {
  const raw = String(control.value ?? '');
  const value = raw.trim();
  if (!value) return { required: true };
  if (raw.length > 80) return { maxlength: { requiredLength: 80, actualLength: raw.length } };
  if (raw.length < 10 || value.length < 10)
    return { minlength: { requiredLength: 10, actualLength: value.length } };
  return null;
};
const nonBlank: ValidatorFn = (control): ValidationErrors | null =>
  String(control.value ?? '').trim() ? null : { required: true };

/** Formulario de alta y edición de publicaciones (CU-03, HU-08/HU-10, RF-PUB-01/03/04/06). */
@Component({
  selector: 'app-publish-product-page',
  imports: [ReactiveFormsModule, ListingImageUpload],
  templateUrl: './publish-product-page.html',
})
export class PublishProductPage {
  private readonly api = inject(ApiClient);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly route = inject(ActivatedRoute);
  protected readonly categories = signal<Category[]>([]);
  protected readonly regions = signal<Region[]>([]);
  protected readonly categoryId = signal('');
  protected readonly regionCode = signal('');
  protected readonly editingId = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly isConflict = signal(false);
  protected readonly saving = signal(false);
  protected readonly apiErrors = signal<Record<string, string>>({});
  private etag: string | null = null;
  private loadedListing: Listing | null = null;
  private readonly imageUpload = viewChild(ListingImageUpload);

  protected readonly form = this.fb.group({
    title: ['', titleValidator],
    description: ['', [nonBlank, Validators.maxLength(4000)]],
    price: ['', [Validators.required, wholeNumber, priceRange]],
    stock: ['', [Validators.required, wholeNumber, Validators.min(0), Validators.max(2147483647)]],
    condition: ['', Validators.required],
    category: ['', Validators.required],
    region: ['', Validators.required],
    commune: ['', Validators.required],
  });

  constructor() {
    this.api.get<Category[]>('/categories').subscribe({
      next: (value) => {
        this.categories.set(value);
        this.syncListingCategory();
      },
      error: () => this.message.set('No se pudieron cargar las categorías.'),
    });
    this.api.get<Region[]>('/regions').subscribe({
      next: (value) => this.regions.set(value),
      error: () => this.message.set('No se pudieron cargar las regiones y comunas.'),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadListing(id);
  }

  protected get descriptionLength(): number {
    return this.form.controls.description.value.length;
  }
  protected get childCategories(): Category[] {
    return this.categories().find((item) => item.id === this.categoryId())?.children ?? [];
  }
  protected get communes(): Region['communes'] {
    return this.regions().find((item) => item.code === this.regionCode())?.communes ?? [];
  }
  protected fieldError(field: keyof typeof this.form.controls): string | null {
    const errorField = field === 'category' ? 'categoryId' : field;
    const apiError = this.apiErrors()[errorField];
    if (apiError) return apiError;
    const control = this.form.controls[field];
    if (!control.touched && !control.dirty) return null;
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('minlength')) return 'El título debe tener al menos 10 caracteres.';
    if (control.hasError('maxlength'))
      return field === 'title'
        ? 'El título no puede superar 80 caracteres.'
        : 'La descripción no puede superar 4.000 caracteres.';
    if (control.hasError('min'))
      return field === 'price' ? 'El precio mínimo es $100.' : 'El stock no puede ser negativo.';
    if (control.hasError('max'))
      return field === 'price'
        ? 'El precio máximo es $99.999.999.'
        : 'El stock excede el máximo admitido.';
    if (control.hasError('wholeNumber')) return 'Ingresa un número entero, sin decimales.';
    return null;
  }

  protected formatPrice(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
    const formatted = digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    input.value = formatted;
    this.form.controls.price.setValue(formatted);
    this.clearApiError('price');
  }

  protected selectCategory(parentId: string): void {
    this.categoryId.set(parentId);
    this.form.controls.category.setValue('');
    this.form.controls.category.markAsTouched();
    this.clearApiError('categoryId');
  }

  protected selectLeaf(categoryId: string): void {
    this.form.controls.category.setValue(categoryId);
    this.form.controls.category.markAsTouched();
    this.clearApiError('categoryId');
  }

  protected selectRegion(code: string): void {
    this.regionCode.set(code);
    this.form.controls.commune.setValue('');
    this.clearApiError('region');
    this.clearApiError('commune');
  }

  protected submit(action: 'draft' | 'publish'): void {
    this.message.set(null);
    this.isConflict.set(false);
    this.apiErrors.set({});
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    const payload = {
      title: values.title.trim(),
      description: values.description,
      price: Number(values.price.replaceAll('.', '')),
      stock: Number(values.stock),
      condition: values.condition,
      categoryId: values.category,
      region: values.region,
      commune: values.commune,
    };
    this.saving.set(true);
    const id = this.editingId();
    const save$ = id
      ? this.api.patchResponse<Listing>(`/listings/${id}`, payload, {
          headers: new HttpHeaders({ 'If-Match': this.etag ?? '' }),
        })
      : this.api.postResponse<Listing>('/listings', payload);
    save$.subscribe({
      next: (response) => {
        const listing = response.body;
        if (!listing) {
          this.saving.set(false);
          return;
        }
        this.editingId.set(listing.id);
        this.etag = response.headers.get('ETag') ?? this.etag;
        this.imageUpload()
          ?.uploadPending(listing.id)
          .subscribe({
            next: (success) => {
              if (!success) {
                this.saving.set(false);
                this.message.set(
                  'No se pudieron subir todas las imágenes. Revisa los errores e inténtalo nuevamente.',
                );
              } else if (action === 'publish' && listing.status === 'BORRADOR')
                this.publishListing(listing.id);
              else {
                this.saving.set(false);
                this.message.set(
                  action === 'draft'
                    ? 'Borrador guardado.'
                    : 'Cambios guardados en la publicación.',
                );
              }
            },
          });
      },
      error: (error: unknown) => this.showApiError(error),
    });
  }

  private publishListing(id: string): void {
    const headers = this.etag ? new HttpHeaders({ 'If-Match': this.etag }) : undefined;
    this.api.post<Listing>(`/listings/${id}/publish`, {}, headers ? { headers } : {}).subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set('¡Producto publicado!');
      },
      error: (error: unknown) => this.showApiError(error),
    });
  }

  private loadListing(id: string): void {
    this.editingId.set(id);
    this.api.getResponse<Listing>(`/listings/${id}`).subscribe({
      next: (response) => {
        const listing = response.body;
        if (!listing) return;
        this.loadedListing = listing;
        this.etag =
          response.headers.get('ETag') ??
          `"${(listing as Listing & { version?: number }).version ?? 0}"`;
        this.syncListingCategory();
        this.regionCode.set(listing.region);
        this.form.patchValue({
          title: listing.title,
          description: listing.description,
          price: listing.price.toLocaleString('es-CL'),
          stock: String(listing.stock),
          condition: listing.condition,
          category: listing.categoryId,
          region: listing.region,
          commune: listing.commune,
        });
      },
      error: (error: unknown) => this.showApiError(error),
    });
  }

  private syncListingCategory(): void {
    if (!this.loadedListing) return;
    const parent = this.categories().find((item) =>
      item.children.some((child) => child.id === this.loadedListing?.categoryId),
    );
    this.categoryId.set(parent?.id ?? '');
  }

  private clearApiError(field: string): void {
    const errors = { ...this.apiErrors() };
    delete errors[field];
    if (errors['categoryId'] && !errors['category']) errors['category'] = errors['categoryId'];
    this.apiErrors.set(errors);
  }
  private showApiError(error: unknown): void {
    this.saving.set(false);
    const problem = toProblem(error);
    const errors = { ...problem.fieldErrors };
    const codeField: Record<string, string> = {
      PRICE_OUT_OF_RANGE: 'price',
      INVALID_LISTING_CATEGORY: 'categoryId',
      INVALID_LISTING_LOCATION: 'commune',
      INVALID_LISTING: 'title',
    };
    const field = codeField[problem.code];
    if (field && !Object.keys(errors).length) errors[field] = problem.detail;
    this.apiErrors.set(errors);
    if (problem.status === 409 || problem.status === 412) {
      this.isConflict.set(true);
      this.message.set(
        'Esta publicación cambió desde que la abriste. Recarga la página antes de guardar tus cambios.',
      );
    } else if (!Object.keys(errors).length) this.message.set(problem.detail);
  }
}
