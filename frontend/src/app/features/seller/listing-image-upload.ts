import { HttpClient, HttpEventType } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { Observable, catchError, concatMap, from, map, of, reduce, switchMap } from 'rxjs';

interface UploadImage {
  file: File;
  preview: string;
  id: string | null;
  progress: number;
  error: string | null;
}

/** Selector accesible y gestor de imágenes de una publicación (HU-09, RF-PUB-02, RN-12). */
@Component({
  selector: 'app-listing-image-upload',
  standalone: true,
  template: `
    <fieldset class="field fieldset image-upload">
      <legend>Fotos del producto</legend>
      <p class="hint" id="image-help">JPG, PNG o WebP; hasta 5 MB por imagen y 8 imágenes.</p>
      <label
        class="image-dropzone"
        [class.image-dropzone--active]="dragging()"
        (dragover)="onDragOver($event)"
        (dragleave)="dragging.set(false)"
        (drop)="onDrop($event)"
      >
        <span>Arrastra tus fotos aquí o selecciona archivos</span>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          aria-describedby="image-help"
          (change)="onSelect($event)"
        />
      </label>
      <ol class="image-list" aria-label="Fotos en orden; la primera es la imagen principal">
        @for (image of images(); track image.preview; let index = $index) {
          <li
            class="image-item"
            [attr.aria-label]="'Foto ' + (index + 1) + (index === 0 ? ', principal' : '')"
          >
            <img [src]="image.preview" [alt]="'Vista previa: ' + image.file.name" />
            <div class="image-info">
              <strong>{{ image.file.name }}</strong>
              @if (index === 0) {
                <span>Imagen principal</span>
              }
              @if (image.progress > 0 && image.progress < 100) {
                <progress
                  [value]="image.progress"
                  max="100"
                  [attr.aria-label]="'Subida de ' + image.file.name"
                >
                  {{ image.progress }}%
                </progress>
              }
              @if (image.error) {
                <p class="field-error" role="alert">{{ image.error }}</p>
              }
            </div>
            <button
              type="button"
              [attr.aria-label]="'Mover ' + image.file.name + ' hacia arriba'"
              [disabled]="index === 0 || busy()"
              (click)="move(index, -1)"
            >
              ↑
            </button>
            <button
              type="button"
              [attr.aria-label]="'Mover ' + image.file.name + ' hacia abajo'"
              [disabled]="index === images().length - 1 || busy()"
              (click)="move(index, 1)"
            >
              ↓
            </button>
            <button
              type="button"
              [attr.aria-label]="'Quitar ' + image.file.name"
              [disabled]="busy()"
              (click)="remove(index)"
            >
              Quitar
            </button>
          </li>
        }
      </ol>
    </fieldset>
  `,
})
export class ListingImageUpload {
  private readonly http = inject(HttpClient);
  readonly listingId = input<string | null>(null);
  protected readonly images = signal<UploadImage[]>([]);
  protected readonly dragging = signal(false);
  protected readonly busy = signal(false);
  private readonly maxFiles = 8;
  private readonly maxBytes = 5 * 1024 * 1024;
  private readonly mimeTypes = ['image/jpeg', 'image/png', 'image/webp'];

  protected onSelect(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    if (inputElement.files) this.addFiles(Array.from(inputElement.files));
    inputElement.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    if (event.dataTransfer?.files) this.addFiles(Array.from(event.dataTransfer.files));
  }

  protected move(index: number, offset: number): void {
    const target = index + offset;
    const values = [...this.images()];
    if (target < 0 || target >= values.length) return;
    [values[index], values[target]] = [values[target]!, values[index]!];
    this.images.set(values);
    this.persistOrder()?.subscribe();
  }

  protected remove(index: number): void {
    const values = [...this.images()];
    const [image] = values.splice(index, 1);
    if (image?.id && this.listingId()) {
      this.http.delete(`/api/v1/listings/${this.listingId()}/images/${image.id}`).subscribe({
        error: () =>
          this.images.update((items) => [
            ...items,
            { ...image, error: 'No se pudo quitar la imagen.' },
          ]),
      });
    }
    if (image) URL.revokeObjectURL(image.preview);
    this.images.set(values);
    this.persistOrder()?.subscribe();
  }

  /** Sube los archivos pendientes en secuencia antes de publicar el borrador. */
  uploadPending(listingId = this.listingId()): Observable<boolean> {
    const pending = this.images().filter((image) => !image.id && !image.error);
    if (!pending.length) return of(true);
    if (!listingId) return of(false);
    this.busy.set(true);
    return from(pending).pipe(
      concatMap((image) => this.uploadOne(listingId, image)),
      reduce((success, current) => success && current, true),
      switchMap((success) => {
        this.busy.set(false);
        if (!success) return of(false);
        return (this.persistOrder(listingId) ?? of(undefined)).pipe(map(() => true));
      }),
      catchError(() => {
        this.busy.set(false);
        return of(false);
      }),
    );
  }

  private addFiles(files: File[]): void {
    const values = [...this.images()];
    for (const file of files) {
      if (values.length >= this.maxFiles) {
        values.push(this.invalid(file, 'No puedes agregar más de 8 imágenes.'));
        continue;
      }
      if (!this.mimeTypes.includes(file.type)) {
        values.push(this.invalid(file, 'Formato no válido. Usa JPG, PNG o WebP.'));
        continue;
      }
      if (file.size > this.maxBytes) {
        values.push(this.invalid(file, 'La imagen debe pesar hasta 5 MB.'));
        continue;
      }
      values.push({ file, preview: URL.createObjectURL(file), id: null, progress: 0, error: null });
    }
    this.images.set(values);
  }

  private invalid(file: File, error: string): UploadImage {
    return { file, preview: '', id: null, progress: 0, error };
  }

  private uploadOne(listingId: string, image: UploadImage): Observable<boolean> {
    const body = new FormData();
    body.append('image', image.file);
    return this.http
      .post<{ id: string }>(`/api/v1/listings/${listingId}/images`, body, {
        observe: 'events',
        reportProgress: true,
      })
      .pipe(
        map((event) => {
          if (event.type === HttpEventType.UploadProgress) {
            image.progress = event.total ? Math.round((event.loaded / event.total) * 100) : 0;
            this.images.update((items) => [...items]);
          }
          if (event.type === HttpEventType.Response && event.body) {
            image.id = event.body.id;
            image.progress = 100;
          }
          return event.type === HttpEventType.Response;
        }),
        reduce((done, current) => done || current, false),
        catchError(() => {
          image.error = 'No se pudo subir esta imagen. Inténtalo nuevamente.';
          this.images.update((items) => [...items]);
          return of(false);
        }),
      );
  }

  private persistOrder(listingId = this.listingId()): Observable<void> | null {
    const id = listingId;
    const imageIds = this.images()
      .filter((image) => image.id)
      .map((image) => image.id!);
    if (id && imageIds.length > 1)
      return this.http.put<void>(`/api/v1/listings/${id}/images/order`, { imageIds });
    return null;
  }
}
