package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceContingencyQueueJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeTransmissionQueueJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceContingencyQueueJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfeTransmissionQueueJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.time.Instant;
import java.util.UUID;

@PersistenceAdapter
class TransmissionQueueRepositoryAdapter implements TransmissionQueuePort {

	private final NfceContingencyQueueJpaRepository nfceJpaRepository;
	private final NfeTransmissionQueueJpaRepository nfeJpaRepository;

	TransmissionQueueRepositoryAdapter(NfceContingencyQueueJpaRepository nfceJpaRepository,
			NfeTransmissionQueueJpaRepository nfeJpaRepository) {
		this.nfceJpaRepository = nfceJpaRepository;
		this.nfeJpaRepository = nfeJpaRepository;
	}

	@Override
	public void enqueue(NfceSaleId nfceSaleId) {
		NfceContingencyQueueJpaEntity entity = NfceContingencyQueueJpaEntity.builder()
				.id(UUID.randomUUID())
				.nfceSaleId(nfceSaleId.value())
				.queuedAt(Instant.now())
				.build();
		entity.setNew(true);
		nfceJpaRepository.save(entity);
	}

	@Override
	public void enqueue(NfeDocumentId nfeDocumentId) {
		NfeTransmissionQueueJpaEntity entity = NfeTransmissionQueueJpaEntity.builder()
				.id(UUID.randomUUID())
				.nfeDocumentId(nfeDocumentId.value())
				.queuedAt(Instant.now())
				.build();
		entity.setNew(true);
		nfeJpaRepository.save(entity);
	}
}
