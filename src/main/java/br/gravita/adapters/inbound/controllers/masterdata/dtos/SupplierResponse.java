package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.SupplierId;
import java.util.UUID;

public record SupplierResponse(UUID id) {
	public static SupplierResponse from(SupplierId id) {
		return new SupplierResponse(id.value());
	}
}
