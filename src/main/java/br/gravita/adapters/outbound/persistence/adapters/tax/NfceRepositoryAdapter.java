package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfceSalePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceSaleJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class NfceRepositoryAdapter implements NfceRepositoryPort {

	private final NfceSaleJpaRepository jpaRepository;
	private final NfceSalePersistenceMapper mapper;

	NfceRepositoryAdapter(NfceSaleJpaRepository jpaRepository, NfceSalePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfceSale save(NfceSale sale) {
		NfceSaleJpaEntity entity = mapper.toEntity(sale);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		NfceSaleJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<NfceSale> findById(NfceSaleId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
