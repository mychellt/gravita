package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record OpenPosSessionCommand(UUID registerId, UUID operatorId, CompanyId companyId,
		BigDecimal openingChangeAmount) {

	public OpenPosSessionCommand {
		Objects.requireNonNull(registerId, "registerId is required");
		Objects.requireNonNull(operatorId, "operatorId is required");
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(openingChangeAmount, "openingChangeAmount is required");
	}
}
