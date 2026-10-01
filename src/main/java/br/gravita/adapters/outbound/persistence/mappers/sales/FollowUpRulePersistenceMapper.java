package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpRuleJpaEntity;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface FollowUpRulePersistenceMapper {

	FollowUpRule map(final FollowUpRuleJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	FollowUpRuleJpaEntity map(final FollowUpRule domain);

	@Mapping(target = "value", source = "id")
	FollowUpRuleId map(final UUID id);
}
