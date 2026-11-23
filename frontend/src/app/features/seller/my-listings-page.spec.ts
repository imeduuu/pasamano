import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MyListingsPage } from './my-listings-page';

describe('Mis publicaciones (RF-PUB-08)', () => {
  let fixture: ComponentFixture<MyListingsPage>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MyListingsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyListingsPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const listing = {
    id: 'listing-1',
    title: 'Libro de cocina chilena',
    price: 12990,
    stock: 3,
    status: 'ACTIVA',
    version: 4,
    images: [
      { url: 'https://images.example/full.jpg', thumbnailUrl: 'https://images.example/thumb.jpg' },
    ],
  };

  it('muestra miniatura, datos y estado con color, y pagina resultados', () => {
    const request = http.expectOne('/api/v1/me/listings?page=0&size=20');
    request.flush({ items: [listing], page: 0, size: 20, hasNext: true });
    fixture.detectChanges();

    const row = fixture.nativeElement.querySelector('[data-listing-id="listing-1"]') as HTMLElement;
    expect(row.textContent).toContain('Libro de cocina chilena');
    expect(row.textContent).toContain('$12.990');
    expect(row.textContent).toContain('3');
    expect(row.querySelector('img')?.getAttribute('src')).toBe('https://images.example/thumb.jpg');
    expect(row.querySelector('[data-status="ACTIVA"]')?.classList.contains('status-active')).toBe(
      true,
    );
    (
      fixture.nativeElement.querySelector(
        'button[aria-label="Página siguiente"]',
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();
    http
      .expectOne('/api/v1/me/listings?page=1&size=20')
      .flush({ items: [], page: 1, size: 20, hasNext: false });
  });

  it('filtra por estado y ofrece edición y acciones válidas según el estado', () => {
    http
      .expectOne('/api/v1/me/listings?page=0&size=20')
      .flush({ items: [listing], page: 0, size: 20, hasNext: false });
    fixture.detectChanges();
    const select = fixture.nativeElement.querySelector('#status-filter') as HTMLSelectElement;
    select.value = 'ACTIVA';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    http
      .expectOne('/api/v1/me/listings?page=0&size=20&status=ACTIVA')
      .flush({ items: [listing], page: 0, size: 20, hasNext: false });
    fixture.detectChanges();
    const row = fixture.nativeElement.querySelector('[data-listing-id="listing-1"]') as HTMLElement;
    expect(row.querySelector('a[aria-label="Editar Libro de cocina chilena"]')).toBeTruthy();
    expect(row.querySelector('button[aria-label="Pausar Libro de cocina chilena"]')).toBeTruthy();
  });

  it('confirma eliminación en un diálogo accesible y envía el ETag', () => {
    http
      .expectOne('/api/v1/me/listings?page=0&size=20')
      .flush({ items: [listing], page: 0, size: 20, hasNext: false });
    fixture.detectChanges();
    (
      fixture.nativeElement.querySelector(
        'button[aria-label="Eliminar Libro de cocina chilena"]',
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('[role="alertdialog"]') as HTMLElement;
    expect(dialog.getAttribute('aria-labelledby')).toBeTruthy();
    expect(dialog.textContent).toContain('¿Eliminar');
    expect(http.match((request) => request.method === 'DELETE')).toHaveLength(0);
    (dialog.querySelector('button[data-confirm]') as HTMLButtonElement).click();
    fixture.detectChanges();
    const deletion = http.expectOne('/api/v1/listings/listing-1');
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.headers.get('If-Match')).toBe('"4"');
    deletion.flush(null, { status: 204, statusText: 'No Content' });
    http
      .expectOne('/api/v1/me/listings?page=0&size=20')
      .flush({ items: [], page: 0, size: 20, hasNext: false });
  });
});
