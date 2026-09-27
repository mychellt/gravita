package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpRuleJpaEntity;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface FollowUpRulePersistenceMapper {

	default FollowUpRule toDomain(final FollowUpRuleJpaEntity entity) {
		return FollowUpRule.of(
				FollowUpRuleId.of(entity.getId()),
				entity.getDaysWithoutContact(),
				entity.getTarget(),
				entity.isNotifyOwner(),
				entity.isActive());
	}

	default FollowUpRuleJpaEntity toEntity(final FollowUpRule domain) {
		return FollowUpRuleJpaEntity.builder()
				.id(domain.getId().value())
				.daysWithoutContact(domain.getDaysWithoutContact())
				.target(domain.getTarget())
				.notifyOwner(domain.isNotifyOwner())
				.active(domain.isActive())
				.build();
	}
}
