package cl.trato.identity.application;

import cl.trato.identity.domain.UserId;

/**
 * Cambio de contraseña estando autenticado (RF-AUT-07).
 *
 * @param userId usuario autenticado
 * @param currentPassword contraseña actual
 * @param newPassword nueva contraseña
 */
public record ChangePasswordCommand(UserId userId, String currentPassword, String newPassword) {

  @Override
  public String toString() {
    return "ChangePasswordCommand[userId=" + userId + "]";
  }
}
