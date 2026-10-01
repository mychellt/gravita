package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpTaskJpaEntity;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface FollowUpTaskPersistenceMapper {

	FollowUpTask map(final FollowUpTaskJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	FollowUpTaskJpaEntity map(final FollowUpTask domain);

	@Mapping(target = "value", source = "id")
	FollowUpTaskId map(final UUID id);
}
