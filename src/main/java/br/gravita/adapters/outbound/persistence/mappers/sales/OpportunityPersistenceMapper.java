package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.OpportunityJpaEntity;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OpportunityPersistenceMapper {

	Opportunity map(final OpportunityJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	OpportunityJpaEntity map(final Opportunity domain);

	@Mapping(target = "value", source = "id")
	OpportunityId map(final UUID id);
}
