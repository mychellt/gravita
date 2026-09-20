package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.core.domain.PlanDomain;
import org.mapstruct.Mapper;

@Mapper
public interface PlanPersistenceMapper {

    PlanDomain map(final PlanJpaEntity entity);

    PlanJpaEntity map(final PlanDomain domain);
}
