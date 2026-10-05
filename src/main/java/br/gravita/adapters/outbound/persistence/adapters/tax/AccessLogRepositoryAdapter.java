package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.AccessLogPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.AccessLogJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@PersistenceAdapter
class AccessLogRepositoryAdapter implements AccessLogRepositoryPort {

	private final AccessLogJpaRepository jpaRepository;
	private final AccessLogPersistenceMapper mapper;

	AccessLogRepositoryAdapter(final AccessLogJpaRepository jpaRepository, final AccessLogPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public AccessLog save(final AccessLog accessLog) {
		return mapper.map(jpaRepository.save(mapper.map(accessLog)));
	}

	@Override
	public Page<AccessLog> search(final GetAccessLogQuery query) {
		final org.springframework.data.domain.Page<AccessLogJpaEntity> result = jpaRepository.search(
				query.userId() == null ? null : query.userId().value(),
				query.dateFrom(),
				query.dateTo(),
				query.ip(),
				query.device(),
				PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "timestamp")));

		return new Page<>(
				result.getContent().stream().map(mapper::map).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements());
	}
}
