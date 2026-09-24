package br.gravita.core.ports.inbound.inventory;

import java.util.UUID;

/**
 * {@code warehouseId} is optional: omit it to sweep every warehouse
 * (UC-M5-11).
 */
public record SuggestReorderQuery(UUID warehouseId) {

	public static SuggestReorderQuery fullSweep() {
		return new SuggestReorderQuery(null);
	}
}
