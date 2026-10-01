package br.gravita.core.ports.outbound.tax;

import java.time.Instant;
import java.util.Objects;

/**
 * What a municipality answered to a cancellation request: either a confirmation (with the time it took effect) or an
 * active rejection. Build it with {@link #confirmed} or {@link #rejected}.
 */
public record NfseCancellationResult(Instant cancelledAt, String rejectionReason) {

	public NfseCancellationResult {
		if ((cancelledAt != null) == (rejectionReason != null)) {
			throw new IllegalArgumentException("exactly one of cancelledAt or rejectionReason must be present");
		}
	}

	public static NfseCancellationResult confirmed(Instant cancelledAt) {
		return new NfseCancellationResult(Objects.requireNonNull(cancelledAt, "cancelledAt"), null);
	}

	public static NfseCancellationResult rejected(String reason) {
		return new NfseCancellationResult(null, Objects.requireNonNull(reason, "reason"));
	}

	public boolean isConfirmed() {
		return cancelledAt != null;
	}
}
