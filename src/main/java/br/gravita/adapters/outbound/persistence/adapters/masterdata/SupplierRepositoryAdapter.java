package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.SupplierPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.SupplierJpaRepository;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.annotations.PersistenceAdapter;
import java.util.Optional;

@PersistenceAdapter
class SupplierRepositoryAdapter implements SupplierRepositoryPort {

	private final SupplierJpaRepository jpaRepository;
	private final SupplierPersistenceMapper mapper;

	SupplierRepositoryAdapter(final SupplierJpaRepository jpaRepository, final SupplierPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Supplier save(final Supplier supplier) {
		final SupplierJpaEntity entity = mapper.map(supplier);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final SupplierJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<Supplier> findById(final SupplierId id) {
		return jpaRepository.findById(id.value()).map(entity -> mapper.map(entity));
	}
}
