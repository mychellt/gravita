package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.CostCenterJpaEntity;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.ports.persistence.CostCenterRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class CostCenterRepositoryAdapter implements CostCenterRepositoryPort {

	private final CostCenterJpaRepository jpaRepository;
	private final CostCenterPersistenceMapper mapper = new CostCenterPersistenceMapper();

	CostCenterRepositoryAdapter(CostCenterJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public CostCenterDomain save(CostCenterDomain model) {
		CostCenterJpaEntity saved = jpaRepository.save(mapper.toEntity(model));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<CostCenterDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<CostCenterDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}

	@Override
	public boolean existsByParentId(UUID parentId) {
		return jpaRepository.existsByParentId(parentId);
	}
}
