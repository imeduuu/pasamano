import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListingImageUpload } from './listing-image-upload';

describe('Subida de imágenes (HU-09, RF-PUB-02, RN-12)', () => {
  let fixture: ComponentFixture<ListingImageUpload>;
  let http: HttpTestingController;
  let component: {
    addFiles: (files: File[]) => void;
    move: (index: number, offset: number) => void;
    images: () => { file: File; error: string | null; id: string | null }[];
    uploadPending: () => import('rxjs').Observable<boolean>;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListingImageUpload],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ListingImageUpload);
    component = fixture.componentInstance as unknown as typeof component;
    fixture.detectChanges();
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });

  afterEach(() => http.verify());

  it('muestra errores por formato, tamaño y más de ocho archivos', () => {
    const files = [
      new File(['x'], 'texto.gif', { type: 'image/gif' }),
      new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.png', { type: 'image/png' }),
      ...Array.from(
        { length: 7 },
        (_, index) => new File(['x'], `foto-${index}.png`, { type: 'image/png' }),
      ),
    ];
    component.addFiles(files);
    fixture.detectChanges();
    expect(component.images()[0]?.error).toContain('Formato');
    expect(component.images()[1]?.error).toContain('5 MB');
    expect(component.images()[8]?.error).toContain('8 imágenes');
    expect(fixture.nativeElement.textContent).toContain('Arrastra tus fotos aquí');
  });

  it('reordena con los controles de teclado accesibles y marca la primera como principal', () => {
    component.addFiles([
      new File(['a'], 'primera.png', { type: 'image/png' }),
      new File(['b'], 'segunda.png', { type: 'image/png' }),
    ]);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[aria-label="Mover segunda.png hacia arriba"]').click();
    fixture.detectChanges();
    expect(component.images().map(({ file }) => file.name)).toEqual(['segunda.png', 'primera.png']);
    expect(fixture.nativeElement.textContent).toContain('Imagen principal');
  });

  it('sube un archivo como multipart image y reporta progreso', () => {
    fixture.componentRef.setInput('listingId', 'listing-1');
    component.addFiles([new File(['foto'], 'foto.jpg', { type: 'image/jpeg' })]);
    fixture.detectChanges();
    let result: boolean | undefined;
    component.uploadPending().subscribe((success) => (result = success));
    const request = http.expectOne('/api/v1/listings/listing-1/images');
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBe(true);
    expect((request.request.body as FormData).has('image')).toBe(true);
    request.event({ type: HttpEventType.UploadProgress, loaded: 5, total: 10 });
    request.flush({ id: 'image-1' });
    expect(result).toBe(true);
    expect(component.images()[0]?.id).toBe('image-1');
  });
});
