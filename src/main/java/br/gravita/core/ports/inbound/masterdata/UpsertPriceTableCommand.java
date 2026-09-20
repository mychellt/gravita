package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpsertPriceTableCommand(
		UUID priceTableId,
		PriceFormation formation,
		LocalDate validFrom,
		LocalDate validTo,
		BigDecimal maxDiscountPercent,
		MaxDiscountBehavior maxDiscountBehavior,
		List<PriceTableEntry> entries) {
}
