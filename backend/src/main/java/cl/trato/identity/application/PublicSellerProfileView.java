package cl.trato.identity.application;

import java.time.Instant;

/** Datos públicos del vendedor; excluye correo y datos personales (RF-USR-03). */
public record PublicSellerProfileView(
    String storeName,
    Instant activatedAt,
    String reputation,
    long completedSales,
    long activeListings) {}
