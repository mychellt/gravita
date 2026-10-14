package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.TransportModalityJpaEntity;
import br.gravita.core.domain.TransportModalityDomain;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TransportModalityPersistenceMapper {

    TransportModalityDomain map(final TransportModalityJpaEntity entity);

    TransportModalityJpaEntity map(final TransportModalityDomain domain);
}
