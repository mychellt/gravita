package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.TransmissionQueueEntry;
import br.gravita.core.domain.tax.TransmissionQueueId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransmissionQueuePort {

	void enqueue(TransmissionQueueId id);

	List<TransmissionQueueEntry> findDue(Instant asOf);

	void reschedule(UUID documentId, int attempts, Instant nextRetryAt);

	void remove(UUID documentId);
}
