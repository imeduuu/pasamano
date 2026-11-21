package cl.trato.identity.domain;

/** Rol de autorización (RF-AUT-09). Equivalencias con el SRS entre paréntesis. */
public enum Role {
  /** Usuario registrado: compra y, si activa su perfil, vende (USUARIO). */
  USER,
  /** Cuenta de moderación (ADMIN). */
  ADMIN
}
