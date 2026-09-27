package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.InteractionJpaEntity;
import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.InteractionId;
import br.gravita.core.domain.sales.OpportunityId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InteractionPersistenceMapper {

	default Interaction toDomain(final InteractionJpaEntity entity) {
		return Interaction.of(
				InteractionId.of(entity.getId()),
				entity.getOpportunityId() != null ? OpportunityId.of(entity.getOpportunityId()) : null,
				entity.getCustomerId(),
				entity.getChannel(),
				entity.getSummary(),
				entity.getTimestamp());
	}

	default InteractionJpaEntity toEntity(final Interaction domain) {
		return InteractionJpaEntity.builder()
				.id(domain.getId().value())
				.opportunityId(domain.getOpportunityId() != null ? domain.getOpportunityId().value() : null)
				.customerId(domain.getCustomerId())
				.channel(domain.getChannel())
				.summary(domain.getSummary())
				.timestamp(domain.getTimestamp())
				.build();
	}
}
