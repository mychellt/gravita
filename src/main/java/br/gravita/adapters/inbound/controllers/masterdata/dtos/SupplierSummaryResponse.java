package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.Supplier;
import java.util.UUID;

public record SupplierSummaryResponse(UUID id, String name, String document) {
	public static SupplierSummaryResponse from(final Supplier supplier) {
		return new SupplierSummaryResponse(supplier.getId().value(), supplier.getName(), supplier.getDocument().number());
	}
}
