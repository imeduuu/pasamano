import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PublishProductPage } from './publish-product-page';

describe('Publicar producto (CU-03, RF-PUB-01/03/04/06)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<PublishProductPage>;
  let root: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PublishProductPage);
    root = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('carga categorías y regiones, y muestra los campos del formulario', () => {
    http
      .expectOne('/api/v1/categories')
      .flush([
        { id: 'parent', name: 'Hogar', children: [{ id: 'leaf', name: 'Cocina', children: [] }] },
      ]);
    http
      .expectOne('/api/v1/regions')
      .flush([
        { code: '13', name: 'Metropolitana', communes: [{ code: '13101', name: 'Santiago' }] },
      ]);
    fixture.detectChanges();
    for (const id of [
      'title',
      'description',
      'price',
      'stock',
      'condition',
      'category',
      'region',
      'commune',
    ]) {
      expect(root.querySelector(`#${id}`), `campo ${id}`).toBeTruthy();
    }
    expect(root.textContent).toContain('0 / 4.000');
    expect(root.textContent).toContain('Guardar borrador');
    expect(root.textContent).toContain('Publicar');
  });

  it('impide enviar datos inválidos y asocia los errores al campo', () => {
    http.expectOne('/api/v1/categories').flush([]);
    http.expectOne('/api/v1/regions').flush([]);
    root.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(root.querySelector('#title-error')?.textContent).toContain('obligatorio');
    expect(root.querySelector('#title')?.getAttribute('aria-describedby')).toContain('title-error');
    http.expectNone('/api/v1/listings');
  });

  it('guarda el borrador en CLP entero y presenta errores Problem Details junto al campo', () => {
    http
      .expectOne('/api/v1/categories')
      .flush([
        { id: 'parent', name: 'Hogar', children: [{ id: 'leaf', name: 'Cocina', children: [] }] },
      ]);
    http
      .expectOne('/api/v1/regions')
      .flush([
        { code: '13', name: 'Metropolitana', communes: [{ code: '13101', name: 'Santiago' }] },
      ]);
    const set = (id: string, value: string) => {
      const input = root.querySelector<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(
        `#${id}`,
      )!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('change'));
    };
    set('title', 'Silla de madera');
    set('description', 'En excelente estado');
    set('price', '25.990');
    set('stock', '2');
    set('condition', 'USADO');
    const page = fixture.componentInstance as unknown as {
      selectCategory: (id: string) => void;
      selectLeaf: (id: string) => void;
      form: { value: { category: string } };
    };
    page.selectCategory('parent');
    fixture.detectChanges();
    page.selectLeaf('leaf');
    set('region', '13');
    fixture.detectChanges();
    set('commune', '13101');
    fixture.detectChanges();
    expect(page.form.value.category).toBe('leaf');
    expect(
      [...root.querySelectorAll('.field-error')].map((item) => `${item.id}: ${item.textContent}`),
    ).toEqual([]);
    root.querySelector<HTMLButtonElement>('[data-action="draft"]')!.click();
    const request = http.expectOne('/api/v1/listings');
    expect(request.request.body).toEqual({
      title: 'Silla de madera',
      description: 'En excelente estado',
      price: 25990,
      stock: 2,
      condition: 'USADO',
      categoryId: 'leaf',
      region: '13',
      commune: '13101',
    });
    request.flush(
      {
        code: 'VALIDATION_ERROR',
        detail: 'Revisa el precio.',
        errors: [{ field: 'price', message: 'Precio fuera de rango.' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();
    expect(root.querySelector('#price-error')?.textContent).toContain('Precio fuera de rango');
  });
});
