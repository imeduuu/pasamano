package cl.trato.geo.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface CommuneJpaRepository extends JpaRepository<CommuneJpaEntity, String> {}
