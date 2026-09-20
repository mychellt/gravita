package br.gravita.masterdata.adapter.in.web.dto;

import br.gravita.masterdata.domain.model.PriceTableId;
import java.util.UUID;

public record PriceTableResponse(UUID id) {
	public static PriceTableResponse from(PriceTableId id) {
		return new PriceTableResponse(id.value());
	}
}
