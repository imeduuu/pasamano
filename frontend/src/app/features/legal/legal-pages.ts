import { Component } from '@angular/core';

/** Versión vigente de los documentos legales; coincide con la que registra el backend. */
export const LEGAL_VERSION = '2026-11-20';

@Component({
  selector: 'app-terms-page',
  template: `
    <article class="auth-card">
      <h1>Términos y condiciones</h1>
      <p class="hint">Versión {{ version }}</p>
      <p>
        Trato es un proyecto de demostración y portafolio. No se venden productos reales y los pagos
        usan exclusivamente el ambiente de pruebas de Transbank: no se mueve dinero real.
      </p>
      <p>
        Al crear una cuenta te comprometes a no publicar contenido ilícito u ofensivo y aceptas que
        la cuenta puede suspenderse por mal uso.
      </p>
    </article>
  `,
})
export class TermsPage {
  protected readonly version = LEGAL_VERSION;
}

@Component({
  selector: 'app-privacy-page',
  template: `
    <article class="auth-card">
      <h1>Política de privacidad</h1>
      <p class="hint">Versión {{ version }}</p>
      <p>
        Tratamos tus datos conforme a la Ley 19.628 y su reforma (Ley 21.719). Solo pedimos lo
        necesario para operar tu cuenta: nombre, correo y, si lo indicas, región, comuna y foto.
      </p>
      <p>
        Tu contraseña se guarda cifrada con un hash irreversible. Registramos la fecha y la versión
        de estos documentos cuando los aceptas. Puedes pedir acceso, rectificación, supresión,
        oposición y portabilidad de tus datos.
      </p>
    </article>
  `,
})
export class PrivacyPage {
  protected readonly version = LEGAL_VERSION;
}
