package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalityIntegrationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.MunicipalityIntegrationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalityIntegrationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import java.util.ArrayList;
import java.util.Optional;

@PersistenceAdapter
class MunicipalityIntegrationRepositoryAdapter implements MunicipalityIntegrationRepositoryPort {

	private final MunicipalityIntegrationJpaRepository jpaRepository;
	private final MunicipalityIntegrationPersistenceMapper mapper;

	MunicipalityIntegrationRepositoryAdapter(MunicipalityIntegrationJpaRepository jpaRepository,
			MunicipalityIntegrationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public MunicipalityIntegration save(MunicipalityIntegration integration) {
		MunicipalityIntegrationJpaEntity entity = jpaRepository.findById(integration.getId().value())
				.map(existing -> {
					// Update the managed row so audit columns (created_at) are preserved.
					MunicipalityIntegrationJpaEntity fresh = mapper.map(integration);
					existing.setStandard(fresh.getStandard());
					existing.setVersion(fresh.getVersion());
					existing.setWebserviceUrl(fresh.getWebserviceUrl());
					existing.setRequiredCertificateType(fresh.getRequiredCertificateType());
					existing.setRequiredFields(new ArrayList<>(fresh.getRequiredFields()));
					existing.setHomologated(fresh.isHomologated());
					return existing;
				})
				.orElseGet(() -> mapper.map(integration));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public Optional<MunicipalityIntegration> findByIbgeCode(String ibgeCode) {
		return jpaRepository.findByIbgeCode(ibgeCode).map(mapper::map);
	}
}
