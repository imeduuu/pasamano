import { APIRequestContext, expect, Page, request } from '@playwright/test';

const APP_URL = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';
const MAILPIT_URL = process.env['MAILPIT_URL'] ?? 'http://localhost:8025';
const PASSWORD = 'caballo-bateria-grapa';
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p5sAAAAASUVORK5CYII=',
  'base64',
);

interface Account {
  email: string;
  accessToken: string;
}

interface Listing {
  id: string;
  title: string;
  version: number;
}

interface Category {
  id: string;
  children: Category[];
}

interface Region {
  code: string;
  communes: { code: string }[];
}

async function json<T>(response: Awaited<ReturnType<APIRequestContext['get']>>): Promise<T> {
  const body = await response.text();
  expect(response.ok(), `${response.url()} respondió ${response.status()}: ${body}`).toBeTruthy();
  return JSON.parse(body) as T;
}

export async function createApiContext(): Promise<APIRequestContext> {
  return request.newContext({ baseURL: APP_URL });
}

export async function createVerifiedAccount(
  api: APIRequestContext,
  name: string,
): Promise<Account> {
  const nonce = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  const email = `e2e-${nonce}@ejemplo.cl`;
  const registered = await api.post('/api/v1/auth/register', {
    data: { name, email, password: PASSWORD, acceptTerms: true },
  });
  await json<{ message: string }>(registered);

  const mailpit = await request.newContext({ baseURL: MAILPIT_URL });
  try {
    let token: string | undefined;
    await expect
      .poll(
        async () => {
          const search = await mailpit.get('/api/v1/search', {
            params: { query: `to:"${email}" subject:"Verifica tu correo"` },
          });
          const result = await json<{ messages: { ID: string }[] }>(search);
          if (!result.messages.length) return undefined;
          const message = await mailpit.get(`/api/v1/message/${result.messages[0]!.ID}`);
          const content = await json<{ Text: string }>(message);
          token = /\/verificar-correo\?token=([A-Za-z0-9_-]+)/.exec(content.Text)?.[1];
          return token;
        },
        { timeout: 30_000, message: `No llegó el correo de verificación para ${email}` },
      )
      .toBeTruthy();

    const verified = await api.post('/api/v1/auth/verify', { data: { token } });
    await json<{ message: string }>(verified);
  } finally {
    await mailpit.dispose();
  }

  const login = await api.post('/api/v1/auth/login', {
    data: { email, password: PASSWORD },
  });
  const session = await json<{ accessToken: string }>(login);
  return { email, accessToken: session.accessToken };
}

export async function createSeller(
  api: APIRequestContext,
  name: string,
  storeName: string,
): Promise<Account> {
  const seller = await createVerifiedAccount(api, name);
  const response = await api.post('/api/v1/me/seller-profile', {
    headers: { Authorization: `Bearer ${seller.accessToken}` },
    data: { storeName, acceptSalesTerms: true },
  });
  await json<{ storeName: string }>(response);
  return seller;
}

export async function createPublishedListing(
  api: APIRequestContext,
  seller: Account,
  details: { title: string; description: string; price: number; condition?: string },
): Promise<Listing> {
  const headers = { Authorization: `Bearer ${seller.accessToken}` };
  const categoriesResponse = await api.get('/api/v1/categories');
  const categories = await json<Category[]>(categoriesResponse);
  const categoryId = categories.flatMap((category) => category.children)[0]?.id;
  expect(categoryId, 'Debe existir una categoría hoja activa').toBeTruthy();

  const regionsResponse = await api.get('/api/v1/regions');
  const regions = await json<Region[]>(regionsResponse);
  const location = regions[0];
  expect(location?.communes[0]?.code, 'Debe existir al menos una comuna').toBeTruthy();

  const createdResponse = await api.post('/api/v1/listings', {
    headers,
    data: {
      title: details.title,
      description: details.description,
      price: details.price,
      stock: 5,
      condition: details.condition ?? 'USADO',
      categoryId,
      region: location!.code,
      commune: location!.communes[0]!.code,
    },
  });
  const created = await json<Listing>(createdResponse);

  const imageResponse = await api.post(`/api/v1/listings/${created.id}/images`, {
    headers,
    multipart: { image: { name: 'producto.png', mimeType: 'image/png', buffer: PNG } },
  });
  await json<{ id: string }>(imageResponse);

  const currentResponse = await api.get(`/api/v1/listings/${created.id}`, { headers });
  const current = await json<Listing>(currentResponse);
  const publishedResponse = await api.post(`/api/v1/listings/${created.id}/publish`, {
    headers: { ...headers, 'If-Match': `"${current.version}"` },
  });
  return json<Listing>(publishedResponse);
}

export async function loginThroughUi(page: Page, email: string): Promise<void> {
  await page.goto('/ingresar');
  await page.getByLabel('Correo electrónico').fill(email);
  await page.getByLabel('Contraseña').fill(PASSWORD);
  await page.getByRole('main').getByRole('button', { name: 'Ingresar' }).click();
  await expect(page).toHaveURL(/\/perfil$/);
}

export async function putCartItem(
  api: APIRequestContext,
  buyer: Account,
  listingId: string,
  quantity: number,
): Promise<void> {
  const response = await api.put(`/api/v1/cart/items/${listingId}`, {
    headers: { Authorization: `Bearer ${buyer.accessToken}` },
    data: { quantity },
  });
  await json(response);
}
