package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.ports.persistence.ChartOfAccountsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ChartOfAccountsRepositoryAdapter implements ChartOfAccountsRepositoryPort {

	private final ChartOfAccountsJpaRepository jpaRepository;
	private final ChartOfAccountsPersistenceMapper mapper = new ChartOfAccountsPersistenceMapper();

	ChartOfAccountsRepositoryAdapter(ChartOfAccountsJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public ChartOfAccountsDomain save(ChartOfAccountsDomain model) {
		ChartOfAccountsJpaEntity saved = jpaRepository.save(mapper.toEntity(model));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<ChartOfAccountsDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<ChartOfAccountsDomain> findAll() {
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
