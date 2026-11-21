package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.application.port.ConsentRepository;
import cl.trato.identity.domain.Consent;
import org.springframework.stereotype.Component;

/** Adaptador JPA del puerto {@link ConsentRepository}. */
@Component
class JpaConsentRepositoryAdapter implements ConsentRepository {

  private final ConsentJpaRepository jpa;
  private final IdentityPersistenceMapper mapper;

  JpaConsentRepositoryAdapter(ConsentJpaRepository jpa, IdentityPersistenceMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public void save(Consent consent) {
    jpa.save(mapper.toEntity(consent));
  }
}
