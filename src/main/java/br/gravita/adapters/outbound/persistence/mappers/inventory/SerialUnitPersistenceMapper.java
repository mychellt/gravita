package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.SerialUnitJpaEntity;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.inventory.SerialUnitId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SerialUnitPersistenceMapper {

	SerialUnit map(final SerialUnitJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	SerialUnitJpaEntity map(final SerialUnit domain);

	@Mapping(target = "value", source = "id")
	SerialUnitId mapSerialUnitId(final UUID id);
}
