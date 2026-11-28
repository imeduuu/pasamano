package cl.trato.identity.application;

import cl.trato.identity.domain.UserId;

/**
 * Edición parcial del perfil (RF-USR-01). Un campo {@code null} no cambia; un texto vacío borra un
 * campo opcional.
 *
 * @param userId usuario autenticado
 * @param name nuevo nombre visible o {@code null}
 * @param region nuevo código CUT regional, vacío para borrar la ubicación o {@code null}
 * @param commune nuevo código CUT comunal, vacío para borrar la ubicación o {@code null}
 * @param photoUrl nueva URL de foto, vacío para borrar o {@code null}
 */
public record UpdateProfileCommand(
    UserId userId, String name, String region, String commune, String photoUrl) {}
