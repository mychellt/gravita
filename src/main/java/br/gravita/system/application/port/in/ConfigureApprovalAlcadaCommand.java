package br.gravita.system.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfigureApprovalAlcadaCommand(
		String module, BigDecimal thresholdValue, BigDecimal thresholdDiscountPercent, UUID approverProfileId) {
}
