package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.StageTransitionJpaEntity;
import br.gravita.core.domain.sales.StageTransition;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StageTransitionPersistenceMapper {

	@Mapping(target = "id.value", source = "id")
	@Mapping(target = "opportunityId.value", source = "opportunityId")
	StageTransition map(final StageTransitionJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "opportunityId", source = "opportunityId.value")
	StageTransitionJpaEntity map(final StageTransition domain);
}
