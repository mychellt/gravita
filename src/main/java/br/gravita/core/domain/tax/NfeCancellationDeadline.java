package br.gravita.core.domain.tax;

import java.time.Duration;
import java.util.Map;

/**
 * UC-M2-04 (§3.2): "within the legal deadline (up to 24h, or the state
 * limit)." The national default is 24h from authorization; this map is the
 * extension point for the states whose SEFAZ regulation grants a longer
 * window (e.g. AM, due to Zona Franca de Manaus logistics) - it's a starting
 * reference, not an exhaustive legal table, and should be extended as new
 * state-specific deadlines are confirmed.
 */
public final class NfeCancellationDeadline {

	private static final Duration DEFAULT_WINDOW = Duration.ofHours(24);

	private static final Map<String, Duration> STATE_WINDOWS = Map.of("AM", Duration.ofHours(48));

	private NfeCancellationDeadline() {
	}

	public static Duration windowFor(final String state) {
		return STATE_WINDOWS.getOrDefault(state, DEFAULT_WINDOW);
	}
}
