package br.gravita.masterdata.application.port.in;

import br.gravita.masterdata.domain.model.MaxDiscountBehavior;
import br.gravita.masterdata.domain.model.PriceFormation;
import br.gravita.masterdata.domain.model.PriceTableEntry;
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
