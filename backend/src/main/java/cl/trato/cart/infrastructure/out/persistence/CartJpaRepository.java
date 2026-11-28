package cl.trato.cart.infrastructure.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {}
