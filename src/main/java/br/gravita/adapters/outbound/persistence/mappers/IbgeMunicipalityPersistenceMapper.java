package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.IbgeMunicipalityJpaEntity;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import org.mapstruct.Mapper;

@Mapper
public interface IbgeMunicipalityPersistenceMapper {

    IbgeMunicipalityDomain map(final IbgeMunicipalityJpaEntity entity);

    IbgeMunicipalityJpaEntity map(final IbgeMunicipalityDomain domain);
}
