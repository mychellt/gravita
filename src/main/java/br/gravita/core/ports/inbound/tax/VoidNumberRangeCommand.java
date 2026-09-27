package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.util.Objects;

public record VoidNumberRangeCommand(CompanyId companyId, String series, Long startNumber, Long endNumber,
		String justification) {

	public VoidNumberRangeCommand {
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(series, "series is required");
		Objects.requireNonNull(startNumber, "startNumber is required");
		Objects.requireNonNull(endNumber, "endNumber is required");
	}
}
