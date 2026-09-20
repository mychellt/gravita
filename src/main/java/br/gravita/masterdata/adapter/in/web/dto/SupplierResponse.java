package br.gravita.masterdata.adapter.in.web.dto;

import br.gravita.masterdata.domain.model.SupplierId;
import java.util.UUID;

public record SupplierResponse(UUID id) {
	public static SupplierResponse from(SupplierId id) {
		return new SupplierResponse(id.value());
	}
}
