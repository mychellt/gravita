package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.CostCenterJpaEntity;
import br.gravita.core.domain.CostCenterDomain;
import org.mapstruct.Mapper;

@Mapper
public interface CostCenterPersistenceMapper {
    CostCenterDomain map(final CostCenterJpaEntity entity);

    CostCenterJpaEntity map(final CostCenterDomain domain);
}
