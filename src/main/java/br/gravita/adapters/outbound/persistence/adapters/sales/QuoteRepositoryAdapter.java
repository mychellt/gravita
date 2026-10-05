package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.QuoteJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.QuotePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.QuoteJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class QuoteRepositoryAdapter implements QuoteRepositoryPort {

	private final QuoteJpaRepository jpaRepository;
	private final QuotePersistenceMapper mapper;

	QuoteRepositoryAdapter(final QuoteJpaRepository jpaRepository, final QuotePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Quote save(final Quote quote) {
		final QuoteJpaEntity entity = mapper.map(quote);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final QuoteJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<Quote> findById(final QuoteId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}
}
