package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.DiscriminationTemplateJpaEntity;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DiscriminationTemplatePersistenceMapper {

	DiscriminationTemplate map(final DiscriminationTemplateJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "serviceCode", source = "serviceCode.value")
	DiscriminationTemplateJpaEntity map(final DiscriminationTemplate domain);

	@Mapping(target = "value", source = "id")
	DiscriminationTemplateId mapDiscriminationTemplateId(final UUID id);

	// Goes through ServiceCode.of so the stored code is validated and normalised to the II.SS form.
	static ServiceCode mapServiceCode(final String raw) {
		return raw == null ? null : ServiceCode.of(raw);
	}
}
