import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ActivateSellerPage } from './activate-seller-page';

describe('Activar perfil de vendedor (CU-15)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ActivateSellerPage>;
  let root: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ActivateSellerPage);
    root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('asocia etiquetas y el enlace a los términos de venta', () => {
    expect(root.querySelector('label[for="seller-store-name"]')).toBeTruthy();
    expect(root.querySelector('label[for="seller-terms"]')).toBeTruthy();
    expect(root.querySelector('a[href="/terminos"]')?.textContent).toContain('términos de venta');
  });

  it('muestra errores accesibles por campo al enviar vacío', () => {
    root.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(root.querySelector('#seller-store-name')?.getAttribute('aria-describedby')).toBe(
      'seller-store-name-error',
    );
    expect(root.querySelector('#seller-terms')?.getAttribute('aria-describedby')).toBe(
      'seller-terms-error',
    );
    http.expectNone('/api/v1/me/seller-profile');
  });

  it('envía la activación y redirige a publicar producto', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const storeName = root.querySelector<HTMLInputElement>('#seller-store-name')!;
    storeName.value = 'Tienda Norte';
    storeName.dispatchEvent(new Event('input'));
    root.querySelector<HTMLInputElement>('#seller-terms')!.click();
    root.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    const request = http.expectOne('/api/v1/me/seller-profile');
    expect(request.request.body).toEqual({ storeName: 'Tienda Norte', acceptSalesTerms: true });
    request.flush({ storeName: 'Tienda Norte', termsVersion: '2026-11-20' });
    fixture.detectChanges();
    expect(navigate).toHaveBeenCalledWith('/publicar-producto');
  });

  it('muestra el conflicto de nombre junto al campo', () => {
    const storeName = root.querySelector<HTMLInputElement>('#seller-store-name')!;
    storeName.value = 'Tienda Norte';
    storeName.dispatchEvent(new Event('input'));
    root.querySelector<HTMLInputElement>('#seller-terms')!.click();
    root.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    http
      .expectOne('/api/v1/me/seller-profile')
      .flush(
        { code: 'STORE_NAME_TAKEN', detail: 'Ese nombre de tienda ya está en uso.' },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();
    expect(root.querySelector('#seller-store-name-error')?.textContent).toContain('ya está en uso');
    expect(root.querySelector('#seller-store-name')?.getAttribute('aria-describedby')).toBe(
      'seller-store-name-error',
    );
  });
});
