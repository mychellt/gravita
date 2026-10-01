package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalityIntegrationJpaEntity;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface MunicipalityIntegrationPersistenceMapper {

	MunicipalityIntegration map(final MunicipalityIntegrationJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	MunicipalityIntegrationJpaEntity map(final MunicipalityIntegration domain);

	@Mapping(target = "value", source = "id")
	MunicipalityIntegrationId mapMunicipalityIntegrationId(final UUID id);
}
