package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.InboundNfeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
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

	private boolean violatesAccessKeyUniqueness(DataIntegrityViolationException e) {
		String message = e.getMostSpecificCause().getMessage();
		return message != null && message.contains("access_key");
	}
}
