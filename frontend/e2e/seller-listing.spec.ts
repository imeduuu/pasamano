import { expect, request, test } from '@playwright/test';

const MAILPIT_URL = process.env['MAILPIT_URL'] ?? 'http://localhost:8025';
const PASSWORD = 'caballo-bateria-grapa';

interface MailpitSearch {
  messages: { ID: string }[];
}

async function verificationLink(email: string): Promise<string> {
  const mailpit = await request.newContext({ baseURL: MAILPIT_URL });
  try {
    let link: string | undefined;
    await expect
      .poll(
        async () => {
          const search = await mailpit.get('/api/v1/search', {
            params: { query: `to:"${email}" subject:"Verifica tu correo"` },
          });
          const body = (await search.json()) as MailpitSearch;
          if (!body.messages.length) return undefined;
          const message = await mailpit.get(`/api/v1/message/${body.messages[0]!.ID}`);
          const text = ((await message.json()) as { Text: string }).Text;
          link = /https?:\/\/\S+\/verificar-correo\?token=[A-Za-z0-9_-]+/.exec(text)?.[0];
          return link;
        },
        { timeout: 20_000, message: 'No llegó el correo de verificación a Mailpit' },
      )
      .toBeTruthy();
    return link!;
  } finally {
    await mailpit.dispose();
  }
}

test('el vendedor publica con dos imágenes, la ve anónimamente y al pausarla deja de ser pública', async ({
  page,
  browser,
}) => {
  const email = `e2e-vendedor-${Date.now()}-${Math.floor(Math.random() * 1e6)}@ejemplo.cl`;
  const title = `Mesa de prueba ${Date.now()}`;

  // Registro y verificación (CU-01).
  await page.goto('/registro');
  await page.getByLabel('Nombre').fill('Ana Vendedora E2E');
  await page.getByLabel('Correo electrónico').fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Repite la contraseña').fill(PASSWORD);
  await page.getByLabel(/Acepto los/).check();
  await page.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(page.getByRole('status')).toContainText('te enviamos un correo');
  const link = new URL(await verificationLink(email));
  await page.goto(link.pathname + link.search);
  await expect(page.getByRole('status')).toContainText('verificada');

  // Ingreso y activación del perfil vendedor (HU-07 / CU-15).
  await page.getByRole('link', { name: 'Ingresar' }).first().click();
  await page.getByLabel('Correo electrónico').fill(email);
  await page.getByLabel('Contraseña').fill(PASSWORD);
  await page.getByRole('main').getByRole('button', { name: 'Ingresar' }).click();
  await expect(page).toHaveURL(/\/perfil$/);
  await expect(page.getByRole('heading', { name: 'Mi perfil' })).toBeVisible();
  await expect(page.getByLabel('Nombre visible')).toHaveValue('Ana Vendedora E2E');
  await page.getByRole('navigation').getByRole('link', { name: 'Vender' }).click();
  await expect(page).toHaveURL(/\/activar-perfil-vendedor$/);
  await page.getByLabel('Nombre de tienda').fill(`Tienda Ana ${Date.now()}`);
  await page.getByLabel(/Acepto los/).check();
  await page.getByRole('button', { name: 'Activar perfil de vendedor' }).click();
  await expect(page).toHaveURL(/\/publicar-producto$/);

  // Publicación y carga de dos imágenes (HU-08/HU-09, RF-PUB-01/02).
  await page.getByLabel('Título').fill(title);
  await page
    .getByLabel('Descripción')
    .fill('Mesa de madera en excelente estado, lista para retirar.');
  await page.getByLabel('Precio (CLP)').fill('45.000');
  await page.getByLabel('Stock disponible').fill('2');
  await page.getByLabel('Condición').selectOption('USADO');

  const parent = page.locator('#category-parent');
  await expect(parent.locator('option').nth(1)).toBeAttached();
  await parent.selectOption({ index: 1 });
  const category = page.locator('#category');
  await expect(category.locator('option').nth(1)).toBeAttached();
  await category.selectOption({ index: 1 });

  const region = page.locator('#region');
  await expect(region.locator('option').nth(1)).toBeAttached();
  await region.selectOption({ index: 1 });
  const commune = page.locator('#commune');
  await expect(commune.locator('option').nth(1)).toBeAttached();
  await commune.selectOption({ index: 1 });

  // PNG válido de 1×1 px: suficiente para probar la ruta real de procesamiento y storage.
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p5sAAAAASUVORK5CYII=',
    'base64',
  );
  await page.locator('input[type="file"]').setInputFiles([
    { name: 'mesa-uno.png', mimeType: 'image/png', buffer: png },
    { name: 'mesa-dos.png', mimeType: 'image/png', buffer: png },
  ]);
  await expect(
    page.getByRole('list', { name: /Fotos en orden/ }).getByRole('listitem'),
  ).toHaveCount(2);
  await page.getByRole('button', { name: 'Publicar', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('¡Producto publicado!');

  await page.getByRole('navigation').getByRole('link', { name: 'Mis publicaciones' }).click();
  const row = page.locator('tr').filter({ hasText: title });
  await expect(row.locator('[data-status="ACTIVA"]')).toBeVisible();
  const editLink = row.getByRole('link', { name: `Editar ${title}` });
  await editLink.click();
  await expect(page).toHaveURL(/\/publicar-producto\//);
  const listingId = new URL(page.url()).pathname.split('/').at(-1)!;

  // Un contexto nuevo garantiza que el detalle se consulta sin cookies del vendedor.
  const anonymousContext = await browser.newContext();
  try {
    const anonymousPage = await anonymousContext.newPage();
    await anonymousPage.goto(`/producto/${listingId}`);
    await expect(anonymousPage.getByRole('heading', { name: title })).toBeVisible();
    await expect(
      anonymousPage.getByRole('group', { name: 'Miniaturas del producto' }).getByRole('button'),
    ).toHaveCount(2);
    await expect(anonymousPage.locator('[data-main-image]')).toHaveAttribute('src', /.+/);

    // Pausa de la publicación (HU-10); el mismo detalle debe dejar de ser accesible anónimamente.
    await page.getByRole('navigation').getByRole('link', { name: 'Mis publicaciones' }).click();
    const activeRow = page.locator('tr').filter({ hasText: title });
    await expect(activeRow.locator('[data-status="ACTIVA"]')).toBeVisible();
    await activeRow.getByRole('button', { name: `Pausar ${title}` }).click();
    await expect(activeRow.locator('[data-status="PAUSADA"]')).toBeVisible();

    await anonymousPage.reload();
    await expect(anonymousPage.getByRole('alert')).toContainText('No se encontró la publicación.');
    await expect(anonymousPage.getByRole('heading', { name: title })).toHaveCount(0);
  } finally {
    await anonymousContext.close();
  }
});
