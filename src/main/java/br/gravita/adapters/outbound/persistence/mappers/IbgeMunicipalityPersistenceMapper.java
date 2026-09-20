package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.IbgeMunicipalityJpaEntity;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface IbgeMunicipalityPersistenceMapper {

    IbgeMunicipalityDomain map(final IbgeMunicipalityJpaEntity entity);

    IbgeMunicipalityJpaEntity map(final IbgeMunicipalityDomain domain);
}
