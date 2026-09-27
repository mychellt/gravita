package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpTaskJpaEntity;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface FollowUpTaskPersistenceMapper {

	default FollowUpTask toDomain(final FollowUpTaskJpaEntity entity) {
		return FollowUpTask.of(
				FollowUpTaskId.of(entity.getId()),
				entity.getOpportunityId(),
				entity.getCustomerId(),
				entity.getDueDate(),
				entity.getOwner(),
				entity.getAlertChannel());
	}

	default FollowUpTaskJpaEntity toEntity(final FollowUpTask domain) {
		return FollowUpTaskJpaEntity.builder()
				.id(domain.getId().value())
				.opportunityId(domain.getOpportunityId())
				.customerId(domain.getCustomerId())
				.dueDate(domain.getDueDate())
				.owner(domain.getOwner())
				.alertChannel(domain.getAlertChannel())
				.build();
	}
}
