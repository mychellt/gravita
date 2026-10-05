package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceContingencyQueueJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceContingencyQueueJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.TransmissionQueueEntry;
import br.gravita.core.domain.tax.TransmissionQueueId;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@PersistenceAdapter
class TransmissionQueueRepositoryAdapter implements TransmissionQueuePort {

	private final NfceContingencyQueueJpaRepository jpaRepository;

	TransmissionQueueRepositoryAdapter(final NfceContingencyQueueJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public void enqueue(final TransmissionQueueId id) {
		final Instant now = Instant.now();
		final NfceContingencyQueueJpaEntity entity = NfceContingencyQueueJpaEntity.builder()
				.id(UUID.randomUUID())
				.documentId(id.value())
				.queuedAt(now)
				.attempts(0)
				.nextRetryAt(now)
				.build();
		entity.setNew(true);
		jpaRepository.save(entity);
	}

	@Override
	public List<TransmissionQueueEntry> findDue(final Instant asOf) {
		return jpaRepository.findByNextRetryAtLessThanEqual(asOf).stream()
				.map(entity -> new TransmissionQueueEntry(entity.getDocumentId(), entity.getAttempts(),
						entity.getNextRetryAt()))
				.toList();
	}

	@Override
	public void reschedule(final UUID documentId, final int attempts, final Instant nextRetryAt) {
		final NfceContingencyQueueJpaEntity entity = jpaRepository.findByDocumentId(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Transmission queue entry not found: " + documentId));
		entity.setAttempts(attempts);
		entity.setNextRetryAt(nextRetryAt);
		jpaRepository.save(entity);
	}

	@Override
	public void remove(final UUID documentId) {
		jpaRepository.findByDocumentId(documentId).ifPresent(jpaRepository::delete);
	}
}
