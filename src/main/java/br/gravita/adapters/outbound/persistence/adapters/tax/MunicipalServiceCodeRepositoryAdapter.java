package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalServiceCodeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalServiceCodeRepositoryPort;

@PersistenceAdapter
class MunicipalServiceCodeRepositoryAdapter implements MunicipalServiceCodeRepositoryPort {

	private final MunicipalServiceCodeJpaRepository jpaRepository;

	MunicipalServiceCodeRepositoryAdapter(MunicipalServiceCodeJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public boolean hasServiceCodeList(String municipalityIbgeCode) {
		return jpaRepository.existsByMunicipalityIbgeAndActiveTrue(municipalityIbgeCode);
	}

	@Override
	public boolean existsByMunicipalityAndServiceCode(String municipalityIbgeCode, String serviceCode) {
		return jpaRepository.existsByMunicipalityIbgeAndServiceCodeAndActiveTrue(municipalityIbgeCode, serviceCode);
	}
}
