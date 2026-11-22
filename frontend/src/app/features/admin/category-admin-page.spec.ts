import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoryAdminPage } from './category-admin-page';

describe('Administración de categorías (CU-18, RF-CAT-02)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<CategoryAdminPage>;
  let root: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CategoryAdminPage);
    root = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('muestra el árbol de dos niveles y permite crear, renombrar, ordenar y desactivar con motivo', () => {
    http.expectOne('/api/v1/categories').flush([
      {
        id: 'root',
        name: 'Hogar',
        slug: 'hogar',
        sortOrder: 1,
        children: [{ id: 'child', name: 'Cocina', slug: 'cocina', sortOrder: 1, children: [] }],
      },
    ]);
    fixture.detectChanges();
    expect(root.textContent).toContain('Hogar');
    expect(root.querySelector<HTMLInputElement>('#name-child')?.value).toBe('Cocina');

    const name = root.querySelector<HTMLInputElement>('#new-category-name')!;
    name.value = 'Jardín';
    name.dispatchEvent(new Event('input'));
    root.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    const create = http.expectOne('/api/v1/admin/categories');
    expect(create.request.body).toEqual({
      name: 'Jardín',
      sortOrder: 2,
      parentId: null,
      reason: null,
    });
    create.flush({ id: 'new', name: 'Jardín', slug: 'jardin', sortOrder: 2, children: [] });
    http.expectOne('/api/v1/categories').flush([]);
  });

  it('guarda renombre y orden y exige un motivo para desactivar', () => {
    http
      .expectOne('/api/v1/categories')
      .flush([{ id: 'root', name: 'Hogar', slug: 'hogar', sortOrder: 1, children: [] }]);
    fixture.detectChanges();
    root.querySelector<HTMLInputElement>('#name-root')!.value = 'Casa';
    root.querySelector<HTMLInputElement>('#name-root')!.dispatchEvent(new Event('input'));
    root.querySelector<HTMLInputElement>('#order-root')!.value = '3';
    root.querySelector<HTMLInputElement>('#order-root')!.dispatchEvent(new Event('input'));
    root.querySelector<HTMLButtonElement>('button[type="button"]')!.click();
    const update = http.expectOne('/api/v1/admin/categories/root');
    expect(update.request.body).toEqual({ name: 'Casa', sortOrder: 3, reason: null });
    update.flush({ id: 'root', name: 'Casa', slug: 'casa', sortOrder: 3, children: [] });
    http
      .expectOne('/api/v1/categories')
      .flush([{ id: 'root', name: 'Casa', slug: 'casa', sortOrder: 3, children: [] }]);
    fixture.detectChanges();

    [...root.querySelectorAll<HTMLButtonElement>('button')]
      .find((button) => button.textContent?.trim() === 'Desactivar')!
      .click();
    fixture.detectChanges();
    expect(root.textContent).toContain('Indica el motivo');
    http.expectNone('/api/v1/admin/categories/root/deactivate');

    const reason = root.querySelector<HTMLInputElement>('#deactivate-reason-root')!;
    reason.value = 'Categoría duplicada';
    reason.dispatchEvent(new Event('input'));
    [...root.querySelectorAll<HTMLButtonElement>('button')]
      .find((button) => button.textContent?.trim() === 'Desactivar')!
      .click();
    const deactivate = http.expectOne('/api/v1/admin/categories/root/deactivate');
    expect(deactivate.request.body).toEqual({ reason: 'Categoría duplicada' });
    deactivate.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/v1/categories').flush([]);
  });
});
