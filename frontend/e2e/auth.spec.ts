import { APIRequestContext, expect, request, test } from '@playwright/test';

const MAILPIT_URL = process.env['MAILPIT_URL'] ?? 'http://localhost:8025';
const PASSWORD = 'caballo-bateria-grapa';

interface MailpitSearch {
  messages: { ID: string; Subject: string }[];
}

/** Espera el correo de verificación en Mailpit y devuelve el enlace que contiene. */
async function verificationLink(mailpit: APIRequestContext, email: string): Promise<string> {
  let link: string | undefined;
  await expect
    .poll(
      async () => {
        const search = await mailpit.get('/api/v1/search', {
          params: { query: `to:"${email}" subject:"Verifica tu correo"` },
        });
        const body = (await search.json()) as MailpitSearch;
        if (body.messages.length === 0) {
          return undefined;
        }
        const message = await mailpit.get(`/api/v1/message/${body.messages[0].ID}`);
        const text = ((await message.json()) as { Text: string }).Text;
        link = /https?:\/\/\S+\/verificar-correo\?token=[A-Za-z0-9_-]+/.exec(text)?.[0];
        return link;
      },
      { timeout: 20_000, message: 'No llegó el correo de verificación a Mailpit' },
    )
    .toBeTruthy();
  return link!;
}

test('registro → verificación por correo (Mailpit) → ingreso → perfil', async ({ page }) => {
  const email = `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@ejemplo.cl`;
  const mailpit = await request.newContext({ baseURL: MAILPIT_URL });

  // 1. Registro (CU-01)
  await page.goto('/registro');
  await expect(page.getByRole('note')).toContainText('Proyecto de demostración');
  await page.getByLabel('Nombre').fill('Ana E2E');
  await page.getByLabel('Correo electrónico').fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Repite la contraseña').fill(PASSWORD);
  await page.getByLabel(/Acepto los/).check();
  await page.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(page.getByRole('status')).toContainText('te enviamos un correo');

  // 2. Verificación con el enlace leído desde la API de Mailpit
  const link = new URL(await verificationLink(mailpit, email));
  await page.goto(link.pathname + link.search);
  await expect(page.getByRole('status')).toContainText('verificada');

  // 3. Ingreso (CU-02)
  await page.getByRole('link', { name: 'Ingresar' }).first().click();
  await expect(page).toHaveURL(/\/ingresar/);
  await page.getByLabel('Correo electrónico').fill(email);
  await page.getByLabel('Contraseña').fill(PASSWORD);
  await page.getByRole('main').getByRole('button', { name: 'Ingresar' }).click();

  // 4. Perfil propio
  await expect(page).toHaveURL(/\/perfil$/);
  await expect(page.getByRole('heading', { name: 'Mi perfil' })).toBeVisible();
  await expect(page.getByLabel('Nombre visible')).toHaveValue('Ana E2E');

  // 5. Al recargar, la sesión se recupera con la cookie HttpOnly (token solo en memoria)
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Mi perfil' })).toBeVisible();
  expect(await page.evaluate(() => JSON.stringify(localStorage))).not.toContain('ey');

  // 6. Cerrar sesión
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Ingresar' })).toBeVisible();

  await mailpit.dispose();
});

test('las rutas protegidas exigen sesión', async ({ page }) => {
  await page.goto('/perfil');

  await expect(page).toHaveURL(/\/ingresar\?returnUrl=%2Fperfil/);
  await expect(page.getByRole('heading', { name: 'Ingresar' })).toBeVisible();
});
