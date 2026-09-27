package br.gravita.core.domain.tax;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A row on the shared transmission/contingency queue (doc §13), as seen by a
 * consumer. {@code documentId} is a raw {@link UUID} rather than a
 * {@link TransmissionQueueId} - the queue is polymorphic across document
 * types (NFC-e sales, NFe documents), so a consumer built for one type
 * (e.g. {@code TransmitNfeUseCase}'s queue consumer) resolves the concrete
 * id itself and simply skips entries that don't belong to it.
 */
public record TransmissionQueueEntry(UUID documentId, int attempts, Instant nextRetryAt) {

	public TransmissionQueueEntry {
		Objects.requireNonNull(documentId, "documentId is required");
		Objects.requireNonNull(nextRetryAt, "nextRetryAt is required");
	}
}
