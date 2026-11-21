import { DEFAULT_APP_CONFIG, loadAppConfig } from './app-config';

function fakeFetch(response: Partial<Response> | Error): typeof fetch {
  return (() =>
    response instanceof Error
      ? Promise.reject(response)
      : Promise.resolve(response as Response)) as typeof fetch;
}

describe('loadAppConfig', () => {
  it('lee la URL base de la API y quita la barra final', async () => {
    const config = await loadAppConfig(
      fakeFetch({
        ok: true,
        json: () => Promise.resolve({ apiBaseUrl: 'https://api.test/api/v1/' }),
      }),
    );

    expect(config.apiBaseUrl).toBe('https://api.test/api/v1');
  });

  it('usa los valores por defecto si el archivo no existe', async () => {
    expect(await loadAppConfig(fakeFetch({ ok: false }))).toEqual(DEFAULT_APP_CONFIG);
  });

  it('usa los valores por defecto si la red falla o el valor es inválido', async () => {
    expect(await loadAppConfig(fakeFetch(new Error('offline')))).toEqual(DEFAULT_APP_CONFIG);
    expect(
      await loadAppConfig(fakeFetch({ ok: true, json: () => Promise.resolve({ apiBaseUrl: '' }) })),
    ).toEqual(DEFAULT_APP_CONFIG);
  });
});
