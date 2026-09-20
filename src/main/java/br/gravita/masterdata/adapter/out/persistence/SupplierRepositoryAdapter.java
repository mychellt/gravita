package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.application.port.out.SupplierRepositoryPort;
import br.gravita.masterdata.domain.model.Supplier;
import br.gravita.masterdata.domain.model.SupplierId;
import br.gravita.shared.PersistenceAdapter;
import java.util.Optional;

@PersistenceAdapter
class SupplierRepositoryAdapter implements SupplierRepositoryPort {

	private final SupplierJpaRepository jpaRepository;
	private final SupplierPersistenceMapper mapper = new SupplierPersistenceMapper();

	SupplierRepositoryAdapter(SupplierJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Supplier save(Supplier supplier) {
		SupplierJpaEntity entity = mapper.toEntity(supplier);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		SupplierJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Supplier> findById(SupplierId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
