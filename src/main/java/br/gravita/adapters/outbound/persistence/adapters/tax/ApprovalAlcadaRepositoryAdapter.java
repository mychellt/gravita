package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.ApprovalAlcadaPersistenceMapper;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.adapters.outbound.persistence.repositories.tax.ApprovalAlcadaJpaRepository;

import java.util.Optional;

@PersistenceAdapter
class ApprovalAlcadaRepositoryAdapter implements ApprovalAlcadaRepositoryPort {

	private final ApprovalAlcadaJpaRepository jpaRepository;
	private final ApprovalAlcadaPersistenceMapper mapper;

	ApprovalAlcadaRepositoryAdapter(ApprovalAlcadaJpaRepository jpaRepository, ApprovalAlcadaPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public ApprovalAlcada save(ApprovalAlcada alcada) {
		ApprovalAlcadaJpaEntity saved = jpaRepository.save(mapper.map(alcada));
		return mapper.map(saved);
	}

	@Override
	public Optional<ApprovalAlcada> findByModule(ApprovalModule module) {
		return jpaRepository.findByModule(module).map(mapper::map);
	}
}
