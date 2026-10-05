package br.gravita.core.domain.masterdata;

import java.util.UUID;

public class PriceTableNotFoundException extends RuntimeException {

	public PriceTableNotFoundException(final UUID priceTableId) {
		super("Price table not found: " + priceTableId);
	}
}
