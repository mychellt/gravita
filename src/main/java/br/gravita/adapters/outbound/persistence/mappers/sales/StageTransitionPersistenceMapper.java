package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.StageTransitionJpaEntity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.domain.sales.StageTransitionId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StageTransitionPersistenceMapper {

	default StageTransition toDomain(final StageTransitionJpaEntity entity) {
		return StageTransition.of(
				StageTransitionId.of(entity.getId()),
				OpportunityId.of(entity.getOpportunityId()),
				entity.getFromStage(),
				entity.getToStage(),
				entity.getTimestamp());
	}

	default StageTransitionJpaEntity toEntity(final StageTransition domain) {
		return StageTransitionJpaEntity.builder()
				.id(domain.getId().value())
				.opportunityId(domain.getOpportunityId().value())
				.fromStage(domain.getFromStage())
				.toStage(domain.getToStage())
				.timestamp(domain.getTimestamp())
				.build();
	}
}
