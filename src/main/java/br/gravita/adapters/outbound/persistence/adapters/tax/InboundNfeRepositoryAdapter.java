package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.InboundNfeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;

@PersistenceAdapter
class InboundNfeRepositoryAdapter implements InboundNfeRepositoryPort {

	private final InboundNfeJpaRepository jpaRepository;
	private final InboundNfePersistenceMapper mapper;

	InboundNfeRepositoryAdapter(InboundNfeJpaRepository jpaRepository, InboundNfePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public InboundNfe save(InboundNfe inboundNfe) {
		InboundNfeJpaEntity entity = mapper.toEntity(inboundNfe);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		try {
			InboundNfeJpaEntity saved = jpaRepository.saveAndFlush(entity);
			return mapper.toDomain(saved);
		} catch (DataIntegrityViolationException e) {
			if (violatesAccessKeyUniqueness(e)) {
				throw new DuplicateResourceException(
						"An NFe with access key " + entity.getAccessKey() + " has already been imported");
			}
			throw e;
		}
	}

	@Override
	public Optional<InboundNfe> findById(InboundNfeId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public Optional<InboundNfe> findByAccessKey(String accessKey) {
		return jpaRepository.findByAccessKey(accessKey).map(mapper::toDomain);
	}

	private boolean violatesAccessKeyUniqueness(DataIntegrityViolationException e) {
		String message = e.getMostSpecificCause().getMessage();
		return message != null && message.contains("access_key");
	}

	@Override
	public List<InboundNfe> findIssuedBetween(Instant from, Instant to) {
		return jpaRepository.findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(from, to).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	public List<InboundNfe> findIssuedByCompanyBetween(CompanyId companyId, Instant from, Instant to) {
		return jpaRepository
				.findByCompanyIdAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(companyId.value(), from,
						to)
				.stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<InboundNfe> findConfirmedByCompanyBetween(CompanyId companyId, Instant from, Instant to) {
		return jpaRepository
				.findByCompanyIdAndStatusAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(
						companyId.value(), InboundNfeStatus.CONFIRMED, from, to)
				.stream().map(mapper::toDomain).toList();
	}
}
