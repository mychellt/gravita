package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class NfeRepositoryAdapter implements NfeRepositoryPort {

	private final NfeJpaRepository jpaRepository;
	private final NfePersistenceMapper mapper;

	NfeRepositoryAdapter(final NfeJpaRepository jpaRepository, final NfePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfeDocument save(final NfeDocument document) {
		final NfeJpaEntity entity = mapper.map(document);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final NfeJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<NfeDocument> findById(final NfeDocumentId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<NfeDocument> findAllByCompany(final CompanyId companyId) {
		return jpaRepository.findByIssuerCompanyId(companyId.value()).stream().map(mapper::map)
				.sorted(Comparator.comparing(NfeRepositoryAdapter::emittedAt).reversed()).toList();
	}

	private static Instant emittedAt(final NfeDocument document) {
		return document.getAuthorizedAt() != null ? document.getAuthorizedAt() : document.getCreatedAt();
	}

	@Override
	public List<NfeDocument> findAuthorizedBetween(final Instant from, final Instant to) {
		return jpaRepository
				.findByStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
						NfeDocumentStatus.AUTHORIZED, from, to)
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<NfeDocument> findAuthorizedByCompanyBetween(final CompanyId companyId, final Instant from, final Instant to) {
		return jpaRepository
				.findByIssuerCompanyIdAndStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
						companyId.value(), NfeDocumentStatus.AUTHORIZED, from, to)
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<NfeDocument> findAuthorizedOrCancelledByCompanyBetween(final CompanyId companyId, final Instant from,
			final Instant to) {
		return jpaRepository
				.findByIssuerCompanyIdAndStatusInAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
						companyId.value(), List.of(NfeDocumentStatus.AUTHORIZED, NfeDocumentStatus.CANCELLED), from,
						to)
				.stream().map(mapper::map).toList();
	}
}
