package br.gravita.system.adapter.out.persistence;

import br.gravita.shared.PersistenceAdapter;
import br.gravita.system.application.port.out.ApprovalAlcadaRepositoryPort;
import br.gravita.system.domain.model.ApprovalAlcada;
import br.gravita.system.domain.model.ApprovalModule;

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
