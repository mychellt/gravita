package br.gravita.core.ports.inbound.inventory;

import java.util.UUID;

public record CheckExpiringLotsQuery(int withinDays, UUID warehouseId) {

	public CheckExpiringLotsQuery {
		if (withinDays < 0) {
			throw new IllegalArgumentException("withinDays must not be negative");
		}
	}

	public static CheckExpiringLotsQuery of(final int withinDays) {
		return new CheckExpiringLotsQuery(withinDays, null);
	}
}
