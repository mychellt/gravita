package br.gravita.core.ports.inbound.inventory;

import java.util.UUID;

/**
 * {@code withinDays} is the configurable alert window from the functional
 * requirement ("Alerta de lotes a vencer em X dias (configurável)", UC-M5-10).
 * {@code warehouseId} is optional: omit it to scan every warehouse.
 */
public record CheckExpiringLotsQuery(int withinDays, UUID warehouseId) {

	public CheckExpiringLotsQuery {
		if (withinDays < 0) {
			throw new IllegalArgumentException("withinDays must not be negative");
		}
	}

	public static CheckExpiringLotsQuery of(int withinDays) {
		return new CheckExpiringLotsQuery(withinDays, null);
	}
}
