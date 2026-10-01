package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.InteractionJpaEntity;
import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.OpportunityId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InteractionPersistenceMapper {

	@Mapping(target = "id.value", source = "id")
	Interaction map(final InteractionJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "opportunityId", source = "opportunityId.value")
	InteractionJpaEntity map(final Interaction domain);

	@Mapping(target = "value", source = "id")
	OpportunityId map(final UUID id);
}
