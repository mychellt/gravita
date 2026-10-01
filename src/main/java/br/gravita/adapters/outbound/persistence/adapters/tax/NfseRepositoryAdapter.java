package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.NfseNumberSequenceJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfsePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseNumberSequenceJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseNumber;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
class NfseRepositoryAdapter implements NfseRepositoryPort {

	/** Series given to the first NFSe of a (company, municipality) until a municipality-specific one is configured. */
	static final String DEFAULT_SERIES = "1";

	private final NfseJpaRepository jpaRepository;
	private final NfseNumberSequenceJpaRepository sequenceJpaRepository;
	private final NfsePersistenceMapper mapper;

	NfseRepositoryAdapter(NfseJpaRepository jpaRepository, NfseNumberSequenceJpaRepository sequenceJpaRepository,
			NfsePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.sequenceJpaRepository = sequenceJpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfseDocument save(NfseDocument document) {
		NfseJpaEntity entity = mapper.toEntity(document);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<NfseDocument> findById(NfseId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public Optional<NfseDocument> findByIdForUpdate(NfseId id) {
		return jpaRepository.findByIdForUpdate(id.value()).map(mapper::toDomain);
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public NfseNumber allocateNextNumber(CompanyId companyId, String municipalityIbgeCode) {
		NfseNumberSequenceJpaEntity sequence = sequenceJpaRepository
				.findByCompanyIdAndMunicipalityIbge(companyId.value(), municipalityIbgeCode)
				.orElseGet(() -> NfseNumberSequenceJpaEntity.builder().id(UUID.randomUUID())
						.companyId(companyId.value()).municipalityIbge(municipalityIbgeCode).series(DEFAULT_SERIES)
						.nextNumber(1L).build());
		NfseNumber allocated = new NfseNumber(sequence.getSeries(), sequence.getNextNumber());
		sequence.setNextNumber(sequence.getNextNumber() + 1);
		sequenceJpaRepository.save(sequence);
		return allocated;
	}

	@Override
	public List<NfseDocument> findAuthorizedBetween(Instant from, Instant to) {
		return jpaRepository
				.findByStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
						NfseStatus.AUTHORIZED, from, to)
				.stream().map(mapper::toDomain).toList();
	}
}
