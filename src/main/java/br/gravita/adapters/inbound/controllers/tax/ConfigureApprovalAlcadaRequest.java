package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.ConfigureApprovalAlcadaCommand;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfigureApprovalAlcadaRequest(
		BigDecimal thresholdValue, BigDecimal thresholdDiscountPercent, @NotNull UUID approverProfileId) {

	public ConfigureApprovalAlcadaCommand toCommand(String module) {
		return new ConfigureApprovalAlcadaCommand(module, thresholdValue, thresholdDiscountPercent, approverProfileId);
	}
}
