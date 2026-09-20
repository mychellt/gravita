package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.core.domain.CustomerDomain;
import org.mapstruct.Mapper;

@Mapper
public interface CustomerPersistenceMapper {
    CustomerDomain map(final CustomerJpaEntity entity);

    CustomerJpaEntity map(final CustomerDomain domain);
}
