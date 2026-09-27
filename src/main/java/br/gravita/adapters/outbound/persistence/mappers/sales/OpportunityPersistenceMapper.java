package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.OpportunityJpaEntity;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OpportunityPersistenceMapper {

	default Opportunity toDomain(final OpportunityJpaEntity entity) {
		return Opportunity.of(
				OpportunityId.of(entity.getId()),
				entity.getCustomerId(),
				entity.getEstimatedValue(),
				entity.getProbability(),
				entity.getExpectedCloseDate(),
				entity.getOwner(),
				entity.getStage());
	}

	default OpportunityJpaEntity toEntity(final Opportunity domain) {
		return OpportunityJpaEntity.builder()
				.id(domain.getId().value())
				.customerId(domain.getCustomerId())
				.estimatedValue(domain.getEstimatedValue())
				.probability(domain.getProbability())
				.expectedCloseDate(domain.getExpectedCloseDate())
				.owner(domain.getOwner())
				.stage(domain.getStage())
				.build();
	}
}
