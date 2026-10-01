package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.RpsId;
import java.util.UUID;

public record IssueRpsResponse(UUID id) {

	public static IssueRpsResponse from(RpsId id) {
		return new IssueRpsResponse(id.value());
	}
}
