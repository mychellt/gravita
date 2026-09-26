package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceContingencyQueueJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceContingencyQueueJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.time.Instant;
import java.util.UUID;

@PersistenceAdapter
class TransmissionQueueRepositoryAdapter implements TransmissionQueuePort {

	private final NfceContingencyQueueJpaRepository jpaRepository;

	TransmissionQueueRepositoryAdapter(NfceContingencyQueueJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public void enqueue(NfceSaleId nfceSaleId) {
		NfceContingencyQueueJpaEntity entity = NfceContingencyQueueJpaEntity.builder()
				.id(UUID.randomUUID())
				.nfceSaleId(nfceSaleId.value())
				.queuedAt(Instant.now())
				.build();
		entity.setNew(true);
		jpaRepository.save(entity);
	}
}
