package br.gravita.system.adapter.in.web;

import br.gravita.system.application.port.in.ConfigureApprovalAlcadaCommand;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfigureApprovalAlcadaRequest(
		BigDecimal thresholdValue, BigDecimal thresholdDiscountPercent, @NotNull UUID approverProfileId) {

	public ConfigureApprovalAlcadaCommand toCommand(String module) {
		return new ConfigureApprovalAlcadaCommand(module, thresholdValue, thresholdDiscountPercent, approverProfileId);
	}
}
