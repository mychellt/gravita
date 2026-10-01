package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.DiscriminationTemplateJpaEntity;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DiscriminationTemplatePersistenceMapper {

	default DiscriminationTemplate toDomain(final DiscriminationTemplateJpaEntity entity) {
		return DiscriminationTemplate.of(DiscriminationTemplateId.of(entity.getId()),
				ServiceCode.of(entity.getServiceCode()), entity.getTemplateText());
	}

	default DiscriminationTemplateJpaEntity toEntity(final DiscriminationTemplate domain) {
		return DiscriminationTemplateJpaEntity.builder()
				.id(domain.getId().value())
				.serviceCode(domain.getServiceCode().value())
				.templateText(domain.getTemplateText())
				.build();
	}
}
