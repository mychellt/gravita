package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** The {@code percent} (0 &lt; percent &le; 100) of a payable's amount charged to one cost center. */
public record CostCenterShare(UUID costCenterId, BigDecimal percent) {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	public CostCenterShare {
		Objects.requireNonNull(costCenterId, "costCenterId is required");
		if (percent == null) {
			throw new BusinessRuleException("percent is required");
		}
		if (percent.signum() <= 0 || percent.compareTo(ONE_HUNDRED) > 0) {
			throw new BusinessRuleException("percent must be between 0 (exclusive) and 100: " + percent);
		}
	}
}
