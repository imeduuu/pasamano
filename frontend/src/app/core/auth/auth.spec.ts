import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { adminGuard, authGuard, guestGuard } from './auth.guards';
import { authInterceptor, isPublicAuthUrl } from './auth.interceptor';
import { UserProfile } from './auth.models';
import { AuthService } from './auth.service';

const PROFILE: UserProfile = {
  id: 'u-1',
  email: 'ana@ejemplo.cl',
  name: 'Ana',
  region: null,
  commune: null,
  photoUrl: null,
  status: 'ACTIVE',
  roles: ['USER'],
  seller: false,
  createdAt: '2026-12-07T12:00:00Z',
};

const token = (value: string) => ({
  accessToken: value,
  tokenType: 'Bearer',
  expiresIn: 900,
  expiresAt: '2026-12-07T12:15:00Z',
});

describe('Sesión (AuthService, interceptor y guards)', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let client: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    localStorage.clear();
  });

  afterEach(() => http.verify());

  async function signIn(): Promise<void> {
    const done = firstValueFrom(auth.login('ana@ejemplo.cl', 'caballo-bateria-grapa'));
    const login = http.expectOne('/api/v1/auth/login');
    expect(login.request.withCredentials).toBe(true);
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush(token('jwt-1'));
    http.expectOne('/api/v1/me').flush(PROFILE);
    await done;
  }

  it('al ingresar guarda el token solo en memoria y carga el perfil', async () => {
    await signIn();

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.accessToken()).toBe('jwt-1');
    expect(auth.user()?.name).toBe('Ana');
    expect(JSON.stringify(localStorage)).not.toContain('jwt-1');
    expect(JSON.stringify(sessionStorage)).not.toContain('jwt-1');
  });

  it('agrega el token de acceso a las llamadas a la API', async () => {
    await signIn();

    client.get('/api/v1/me').subscribe();

    expect(http.expectOne('/api/v1/me').request.headers.get('Authorization')).toBe('Bearer jwt-1');
  });

  it('no agrega el token a llamadas fuera de la API', async () => {
    await signIn();

    client.get('/app-config.json').subscribe();

    expect(http.expectOne('/app-config.json').request.headers.has('Authorization')).toBe(false);
  });

  it('si el token venció, renueva de forma transparente y reintenta una vez', async () => {
    await signIn();
    let result: unknown;
    client.get('/api/v1/me').subscribe((body) => (result = body));

    http.expectOne('/api/v1/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    const refresh = http.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.withCredentials).toBe(true);
    refresh.flush(token('jwt-2'));
    const retry = http.expectOne('/api/v1/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer jwt-2');
    retry.flush(PROFILE);

    expect(result).toEqual(PROFILE);
    expect(auth.accessToken()).toBe('jwt-2');
  });

  it('las renovaciones concurrentes comparten una sola petición', async () => {
    await signIn();

    client.get('/api/v1/me').subscribe();
    client.get('/api/v1/me').subscribe();
    http.match('/api/v1/me').forEach((r) => r.flush({}, { status: 401, statusText: 'x' }));

    const refreshes = http.match('/api/v1/auth/refresh');
    expect(refreshes).toHaveLength(1);
    refreshes[0].flush(token('jwt-2'));
    http.match('/api/v1/me').forEach((r) => r.flush(PROFILE));
  });

  it('si la renovación falla, cierra la sesión y vuelve al login', async () => {
    await signIn();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let failed = false;
    client.get('/api/v1/me').subscribe({ error: () => (failed = true) });

    http.expectOne('/api/v1/me').flush({}, { status: 401, statusText: 'x' });
    http.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'x' });

    expect(failed).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/ingresar'], expect.anything());
  });

  it('otros errores no disparan la renovación', async () => {
    await signIn();
    let status = 0;
    client.get('/api/v1/me').subscribe({ error: (e: { status: number }) => (status = e.status) });

    http.expectOne('/api/v1/me').flush({}, { status: 403, statusText: 'Forbidden' });

    expect(status).toBe(403);
    http.expectNone('/api/v1/auth/refresh');
  });

  it('al iniciar recupera la sesión desde la cookie y, si no hay, queda como visitante', async () => {
    const restored = auth.restoreSession();
    http.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'x' });
    await restored;

    expect(auth.isAuthenticated()).toBe(false);
  });

  it('al iniciar con una cookie válida carga el perfil', async () => {
    const restored = auth.restoreSession();
    http.expectOne('/api/v1/auth/refresh').flush(token('jwt-9'));
    http.expectOne('/api/v1/me').flush(PROFILE);
    await restored;

    expect(auth.user()?.email).toBe('ana@ejemplo.cl');
  });

  it('cerrar sesión revoca el token en el servidor y limpia el estado', async () => {
    await signIn();

    const done = firstValueFrom(auth.logout(), { defaultValue: undefined });
    const logout = http.expectOne('/api/v1/auth/logout');
    expect(logout.request.headers.get('Authorization')).toBe('Bearer jwt-1');
    expect(logout.request.withCredentials).toBe(true);
    logout.flush(null, { status: 204, statusText: 'No Content' });
    await done;

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.user()).toBeNull();
  });

  it('distingue los endpoints públicos de autenticación', () => {
    expect(isPublicAuthUrl('/api/v1/auth/login')).toBe(true);
    expect(isPublicAuthUrl('/api/v1/auth/refresh')).toBe(true);
    expect(isPublicAuthUrl('/api/v1/auth/logout')).toBe(false);
    expect(isPublicAuthUrl('/api/v1/me')).toBe(false);
  });

  describe('guards', () => {
    const state = { url: '/perfil' } as never;

    it('sin sesión, las rutas protegidas llevan al login con la ruta de retorno', () => {
      const result = TestBed.runInInjectionContext(() => authGuard({} as never, state));

      expect(result).toBeInstanceOf(UrlTree);
      expect(String(result)).toBe('/ingresar?returnUrl=%2Fperfil');
    });

    it('el guard ADMIN permite admins y redirige usuarios a su perfil', async () => {
      await signIn();
      expect(
        String(
          TestBed.runInInjectionContext(() =>
            adminGuard({} as never, { url: '/admin/categorias' } as never),
          ),
        ),
      ).toBe('/perfil');
      auth.user.set({ ...PROFILE, roles: ['ADMIN'] });
      expect(
        TestBed.runInInjectionContext(() =>
          adminGuard({} as never, { url: '/admin/categorias' } as never),
        ),
      ).toBe(true);
    });

    it('con sesión, las rutas protegidas se abren y las de visitante llevan al perfil', async () => {
      await signIn();

      expect(TestBed.runInInjectionContext(() => authGuard({} as never, state))).toBe(true);
      expect(String(TestBed.runInInjectionContext(() => guestGuard({} as never, state)))).toBe(
        '/perfil',
      );
    });

    it('sin sesión, las rutas de visitante se abren', () => {
      expect(TestBed.runInInjectionContext(() => guestGuard({} as never, state))).toBe(true);
    });
  });
});
