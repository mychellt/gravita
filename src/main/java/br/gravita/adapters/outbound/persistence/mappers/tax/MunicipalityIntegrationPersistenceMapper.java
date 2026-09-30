package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalityIntegrationJpaEntity;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import java.util.ArrayList;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface MunicipalityIntegrationPersistenceMapper {

	default MunicipalityIntegration toDomain(final MunicipalityIntegrationJpaEntity entity) {
		return MunicipalityIntegration.of(
				MunicipalityIntegrationId.of(entity.getId()),
				entity.getIbgeCode(),
				entity.getStandard(),
				entity.getVersion(),
				entity.getWebserviceUrl(),
				entity.getRequiredCertificateType(),
				entity.getRequiredFields(),
				entity.isHomologated());
	}

	default MunicipalityIntegrationJpaEntity toEntity(final MunicipalityIntegration domain) {
		return MunicipalityIntegrationJpaEntity.builder()
				.id(domain.getId().value())
				.ibgeCode(domain.getIbgeCode())
				.standard(domain.getStandard())
				.version(domain.getVersion())
				.webserviceUrl(domain.getWebserviceUrl())
				.requiredCertificateType(domain.getRequiredCertificateType())
				.requiredFields(new ArrayList<>(domain.getRequiredFields()))
				.homologated(domain.isHomologated())
				.build();
	}
}
