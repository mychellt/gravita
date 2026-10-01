package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountLineEmbeddable;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PhysicalCountPersistenceMapper {

	PhysicalCount map(final PhysicalCountJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	PhysicalCountJpaEntity map(final PhysicalCount domain);

	PhysicalCountLine map(final PhysicalCountLineEmbeddable embeddable);

	PhysicalCountLineEmbeddable map(final PhysicalCountLine line);

	@Mapping(target = "value", source = "id")
	PhysicalCountId mapPhysicalCountId(final UUID id);
}
