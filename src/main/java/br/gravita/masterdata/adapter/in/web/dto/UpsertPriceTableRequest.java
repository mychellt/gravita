package br.gravita.masterdata.adapter.in.web.dto;

import br.gravita.masterdata.application.port.in.UpsertPriceTableCommand;
import br.gravita.masterdata.domain.model.MaxDiscountBehavior;
import br.gravita.masterdata.domain.model.PriceFormation;
import br.gravita.masterdata.domain.model.PriceTableEntry;
import br.gravita.masterdata.domain.model.ProductOrClassRef;
import br.gravita.masterdata.domain.model.ProductOrClassRefType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpsertPriceTableRequest(
		@NotNull PriceFormation formation,
		@NotNull LocalDate validFrom,
		LocalDate validTo,
		BigDecimal maxDiscountPercent,
		MaxDiscountBehavior maxDiscountBehavior,
		List<@Valid EntryRequest> entries) {

	public UpsertPriceTableCommand toCommand(UUID priceTableId) {
		return new UpsertPriceTableCommand(
				priceTableId,
				formation,
				validFrom,
				validTo,
				maxDiscountPercent,
				maxDiscountBehavior,
				entries == null ? List.of() : entries.stream().map(EntryRequest::toDomain).toList());
	}

	public record EntryRequest(@NotNull ProductOrClassRefType refType, @NotNull String referenceId,
			@NotNull BigDecimal value) {

		PriceTableEntry toDomain() {
			return new PriceTableEntry(new ProductOrClassRef(refType, referenceId), value);
		}
	}
}
