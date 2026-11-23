package cl.trato.geo.application;

import java.util.List;

/** Vista pública de una región con sus comunas. */
public record RegionView(String code, String name, List<CommuneView> communes) {}
