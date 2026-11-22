import { HttpErrorResponse } from '@angular/common/http';
import { NETWORK_ERROR, UNEXPECTED_ERROR, toProblem } from './problem';

describe('toProblem (Problem Details RFC 9457)', () => {
  it('lee el código, el detalle y los errores por campo', () => {
    const problem = toProblem(
      new HttpErrorResponse({
        status: 400,
        error: {
          code: 'VALIDATION_ERROR',
          detail: 'Algunos datos no son válidos.',
          errors: [
            { field: 'email', message: 'El correo electrónico no es válido.' },
            { field: 'email', message: 'otro' },
          ],
        },
      }),
    );

    expect(problem).toEqual({
      status: 400,
      code: 'VALIDATION_ERROR',
      detail: 'Algunos datos no son válidos.',
      fieldErrors: { email: 'El correo electrónico no es válido.' },
    });
  });

  it('sin conexión muestra un mensaje de red', () => {
    expect(toProblem(new HttpErrorResponse({ status: 0 })).detail).toBe(NETWORK_ERROR);
  });

  it('nunca muestra textos técnicos si la respuesta no es un problema', () => {
    const problem = toProblem(new HttpErrorResponse({ status: 502, error: '<html>Bad Gateway' }));

    expect(problem.code).toBe('HTTP_502');
    expect(problem.detail).toBe(UNEXPECTED_ERROR);
    expect(toProblem(new Error('boom')).detail).toBe(UNEXPECTED_ERROR);
  });
});
