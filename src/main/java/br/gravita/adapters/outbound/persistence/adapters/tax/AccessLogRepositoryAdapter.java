package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.AccessLogPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.AccessLogJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;

@PersistenceAdapter
class AccessLogRepositoryAdapter implements AccessLogRepositoryPort {

	private final AccessLogJpaRepository jpaRepository;
	private final AccessLogPersistenceMapper mapper;

	AccessLogRepositoryAdapter(AccessLogJpaRepository jpaRepository, AccessLogPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public AccessLog save(AccessLog accessLog) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(accessLog)));
	}
}
