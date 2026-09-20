package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.ApprovalAlcadaPersistenceMapper;
import br.gravita.core.domain.shared.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.system.adapter.out.persistence.ApprovalAlcadaJpaRepository;

import java.util.Optional;

@PersistenceAdapter
class ApprovalAlcadaRepositoryAdapter implements ApprovalAlcadaRepositoryPort {

	private final ApprovalAlcadaJpaRepository jpaRepository;
	private final ApprovalAlcadaPersistenceMapper mapper = new ApprovalAlcadaPersistenceMapper();

	ApprovalAlcadaRepositoryAdapter(ApprovalAlcadaJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public ApprovalAlcada save(ApprovalAlcada alcada) {
		ApprovalAlcadaJpaEntity saved = jpaRepository.save(mapper.toEntity(alcada));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<ApprovalAlcada> findByModule(ApprovalModule module) {
		return jpaRepository.findByModule(module).map(mapper::toDomain);
	}
}
