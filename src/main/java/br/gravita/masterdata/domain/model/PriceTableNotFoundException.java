package br.gravita.masterdata.domain.model;

import java.util.UUID;

public class PriceTableNotFoundException extends RuntimeException {

	public PriceTableNotFoundException(UUID priceTableId) {
		super("Price table not found: " + priceTableId);
	}
}
