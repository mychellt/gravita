package br.gravita.core.ports.inbound.inventory;

import java.util.UUID;

public record SuggestReorderQuery(UUID warehouseId) {

	public static SuggestReorderQuery fullSweep() {
		return new SuggestReorderQuery(null);
	}
}
