package br.gravita.adapters.inbound.scheduling.tax;

import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.domain.tax.TransmissionQueueEntry;
import br.gravita.core.ports.inbound.tax.TransmitNfeCommand;
import br.gravita.core.ports.inbound.tax.TransmitNfeUseCase;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the shared transmission queue and drives {@link TransmitNfeUseCase}
 * (UC-M2-03) - the "asynchronous submission with polling" the use case
 * description calls for is this poll loop, not a poll inside a single
 * transmission attempt. Owns the retry/backoff (AC2) and SVC-AN/SVC-RS
 * contingency-switch (AC3) policy, since those are properties of "how many
 * times has this document failed", not of a single attempt.
 *
 * <p>The queue is shared with NFC-e contingency entries (see
 * {@link TransmissionQueuePort}'s javadoc); an entry whose id doesn't
 * resolve to an {@link NfeDocument} belongs to that other consumer (not yet
 * built) and is silently left for it.
 */
@Component
public class TransmissionQueueConsumer {

	static final int CONTINGENCY_THRESHOLD_ATTEMPTS = 3;
	static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
	static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

	private final TransmissionQueuePort transmissionQueuePort;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final TransmitNfeUseCase transmitNfeUseCase;

	public TransmissionQueueConsumer(TransmissionQueuePort transmissionQueuePort, NfeRepositoryPort nfeRepositoryPort,
			TransmitNfeUseCase transmitNfeUseCase) {
		this.transmissionQueuePort = transmissionQueuePort;
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.transmitNfeUseCase = transmitNfeUseCase;
	}

	@Scheduled(fixedDelayString = "${gravita.tax.transmission-queue.poll-interval-ms:30000}")
	public void pollAndTransmit() {
		for (TransmissionQueueEntry entry : transmissionQueuePort.findDue(Instant.now())) {
			processEntry(entry);
		}
	}

	void processEntry(TransmissionQueueEntry entry) {
		if (nfeRepositoryPort.findById(NfeDocumentId.of(entry.documentId())).isEmpty()) {
			return;
		}
		try {
			transmitNfeUseCase.execute(new TransmitNfeCommand(entry.documentId()));
		} catch (SefazUnavailableException unavailable) {
			handleTimeout(entry);
		}
	}

	private void handleTimeout(TransmissionQueueEntry entry) {
		int attempts = entry.attempts() + 1;
		transmissionQueuePort.reschedule(entry.documentId(), attempts, Instant.now().plus(backoff(attempts)));

		if (attempts >= CONTINGENCY_THRESHOLD_ATTEMPTS) {
			nfeRepositoryPort.findById(NfeDocumentId.of(entry.documentId()))
					.filter(document -> !document.isContingencyMode())
					.ifPresent(document -> nfeRepositoryPort.save(document.switchToContingency()));
		}
	}

	static Duration backoff(int attempts) {
		long factor = 1L << Math.min(Math.max(attempts, 1) - 1, 10);
		long seconds = Math.min(BASE_BACKOFF.getSeconds() * factor, MAX_BACKOFF.getSeconds());
		return Duration.ofSeconds(seconds);
	}
}
