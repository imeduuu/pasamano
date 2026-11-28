package cl.trato.search.domain;

import java.util.UUID;

/** Public listing fields returned by RF-BUS-01 to RF-BUS-04. */
public record SearchListing(
    UUID id,
    String title,
    long price,
    String thumbnail,
    String region,
    String commune,
    String storeName,
    String reputation) {}
