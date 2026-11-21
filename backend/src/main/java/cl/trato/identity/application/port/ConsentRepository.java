package cl.trato.identity.application.port;

import cl.trato.identity.domain.Consent;

/** Puerto de salida: registro de consentimientos (RNF-LEG-01). */
public interface ConsentRepository {

  void save(Consent consent);
}
