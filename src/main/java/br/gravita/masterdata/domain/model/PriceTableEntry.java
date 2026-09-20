package br.gravita.masterdata.domain.model;

import br.gravita.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;

public record PriceTableEntry(ProductOrClassRef ref, BigDecimal value) {

	public PriceTableEntry {
		Objects.requireNonNull(ref, "Entry reference is required");
		if (value == null) {
			throw new BusinessRuleException("Entry value is required");
		}
	}
}
