import { randomBytes } from 'node:crypto';
import { expect, test } from '@playwright/test';
import {
  createApiContext,
  createPublishedListing,
  createSeller,
  createVerifiedAccount,
  loginThroughUi,
  putCartItem,
} from './support/api-fixtures';

function searchKeyword(): string {
  return [...randomBytes(12)].map((value) => String.fromCharCode(97 + (value % 26))).join('');
}

test('el comprador busca, filtra y ordena productos, y gestiona el carrito agrupado (HU-12, HU-15)', async ({
  page,
}) => {
  const api = await createApiContext();
  try {
    const keyword = searchKeyword();
    const buyer = await createVerifiedAccount(api, 'Comprador E2E');
    const firstSeller = await createSeller(api, 'Vendedor E2E Uno', `Tienda Uno ${Date.now()}`);
    const secondSeller = await createSeller(api, 'Vendedor E2E Dos', `Tienda Dos ${Date.now()}`);

    const target = await createPublishedListing(api, firstSeller, {
      title: `Silla ${keyword} roble`,
      description: 'Silla de prueba para validar la búsqueda y el carrito.',
      price: 12_000,
    });
    const cheaper = await createPublishedListing(api, firstSeller, {
      title: `Silla ${keyword} económica`,
      description: 'Publicación de menor precio para probar el filtro.',
      price: 5_000,
    });
    const otherSellerItem = await createPublishedListing(api, secondSeller, {
      title: `Silla ${keyword} respaldo alto`,
      description: 'Segundo vendedor para comprobar la agrupación del carrito.',
      price: 18_000,
    });
    await putCartItem(api, buyer, otherSellerItem.id, 1);

    await loginThroughUi(page, buyer.email);
    await page.goto('/buscar');
    await page.locator('#search-query').fill(keyword);
    await page
      .locator('.search-page form[role="search"]')
      .getByRole('button', { name: 'Buscar', exact: true })
      .click();
    await expect(page).toHaveURL(new RegExp(`[?&]q=${keyword}`));
    await expect(page.locator('.cards a.card')).toHaveCount(3);

    const filterToggle = page.locator('[data-filter-toggle]');
    if (await filterToggle.isVisible()) await filterToggle.click();
    const minimumPrice = page.locator('#minPrice');
    await minimumPrice.fill('10000');
    await minimumPrice.press('Tab');
    await expect(page).toHaveURL(/minPrice=10000/);
    await page.locator('#condition').selectOption('USADO');
    await expect(page).toHaveURL(/condition=USADO/);
    await page.locator('#sort').selectOption('price_asc');
    await expect(page).toHaveURL(/sort=price_asc/);

    const cards = page.locator('.cards a.card');
    await expect(cards).toHaveCount(2);
    await expect(cards.first()).toContainText(target.title);
    await expect(cards.nth(1)).toContainText(otherSellerItem.title);
    await expect(page.getByRole('link', { name: new RegExp(cheaper.title) })).toHaveCount(0);

    await cards.first().click();
    await expect(page.getByRole('heading', { name: target.title })).toBeVisible();
    await page.locator('#listing-quantity').selectOption('2');
    await page.getByRole('button', { name: 'Agregar al carrito' }).click();
    await expect(page.getByRole('status')).toContainText('Se agregó al carrito.');

    await page.goto('/carrito');
    await expect(page.getByRole('heading', { name: 'Tu carrito' })).toBeVisible();
    await expect(page.locator('.seller-group')).toHaveCount(2);
    const targetItem = page.locator('.cart-item').filter({ hasText: target.title });
    const otherItem = page.locator('.cart-item').filter({ hasText: otherSellerItem.title });
    await expect(targetItem.getByLabel('Cantidad')).toHaveValue('2');
    await expect(otherItem).toBeVisible();

    const quantityUpdate = page.waitForResponse(
      (response) =>
        response.url().endsWith(`/api/v1/cart/items/${target.id}`) &&
        response.request().method() === 'PUT',
    );
    const quantityInput = targetItem.getByLabel('Cantidad');
    await quantityInput.fill('3');
    await quantityInput.press('Tab');
    const quantityResponse = await quantityUpdate;
    expect(quantityResponse.status(), await quantityResponse.text()).toBe(200);
    await expect(targetItem.getByLabel('Cantidad')).toHaveValue('3');
    const removal = page.waitForResponse(
      (response) =>
        response.url().endsWith(`/api/v1/cart/items/${target.id}`) &&
        response.request().method() === 'DELETE',
    );
    await targetItem.getByRole('button', { name: `Quitar ${target.title} del carrito` }).click();
    const removalResponse = await removal;
    expect(removalResponse.status()).toBe(204);
    await expect(page.locator('.cart-item').filter({ hasText: target.title })).toHaveCount(0);
    await expect(
      page.locator('.cart-item').filter({ hasText: otherSellerItem.title }),
    ).toBeVisible();
    await expect(page.locator('.seller-group')).toHaveCount(1);
  } finally {
    await api.dispose();
  }
});

test('el vendedor no puede agregar su propia publicación al carrito (RF-CAR-02)', async ({
  page,
}) => {
  const api = await createApiContext();
  try {
    const seller = await createSeller(api, 'Vendedor Propio E2E', `Tienda Propia ${Date.now()}`);
    const listing = await createPublishedListing(api, seller, {
      title: `Publicación propia E2E ${Date.now()}`,
      description: 'Publicación del mismo vendedor que inició sesión.',
      price: 9_000,
    });

    await loginThroughUi(page, seller.email);
    await page.goto(`/producto/${listing.id}`);
    await expect(page.getByRole('heading', { name: listing.title })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Agregar al carrito' })).toHaveCount(0);

    const ownItemResponse = await api.put(`/api/v1/cart/items/${listing.id}`, {
      headers: { Authorization: `Bearer ${seller.accessToken}` },
      data: { quantity: 1 },
    });
    expect(ownItemResponse.status()).toBe(403);
  } finally {
    await api.dispose();
  }
});

test('el carrito advierte cuando el vendedor pausa una publicación agregada (HU-10, HU-15)', async ({
  page,
}) => {
  const api = await createApiContext();
  try {
    const buyer = await createVerifiedAccount(api, 'Comprador E2E Pausa');
    const seller = await createSeller(api, 'Vendedor E2E Pausa', `Tienda Pausa ${Date.now()}`);
    const listing = await createPublishedListing(api, seller, {
      title: `Producto pausado E2E ${Date.now()}`,
      description: 'Publicación que será pausada después de agregarla al carrito.',
      price: 15_000,
    });

    await loginThroughUi(page, buyer.email);
    await page.goto(`/producto/${listing.id}`);
    await expect(page.getByRole('heading', { name: listing.title })).toBeVisible();
    await page.getByRole('button', { name: 'Agregar al carrito' }).click();
    await expect(page.getByRole('status')).toContainText('Se agregó al carrito.');

    const paused = await api.post(`/api/v1/listings/${listing.id}/pause`, {
      headers: {
        Authorization: `Bearer ${seller.accessToken}`,
        'If-Match': `"${listing.version}"`,
      },
    });
    expect(paused.ok(), await paused.text()).toBeTruthy();

    await page.goto('/carrito');
    await expect(page.getByRole('heading', { name: 'Tu carrito' })).toBeVisible();
    await expect(page.getByRole('status')).toContainText(
      'Esta publicación está pausada y no se incluye en el total.',
    );
    await expect(page.getByText('Subtotal no incluido')).toBeVisible();
  } finally {
    await api.dispose();
  }
});
