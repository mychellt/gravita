package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.InterstateIcmsRateDomain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record InterstateIcmsRateImportRequest(@NotEmpty List<@Valid Row> rates) {

	public record Row(
			@NotBlank @Pattern(regexp = "[A-Z]{2}") String originState,
			@NotBlank @Pattern(regexp = "[A-Z]{2}") String destinationState,
			@NotNull @PositiveOrZero BigDecimal ratePercent) {
	}

	public List<InterstateIcmsRateDomain> toDomainList() {
		return rates.stream()
				.map(row -> InterstateIcmsRateDomain.builder()
						.originState(row.originState())
						.destinationState(row.destinationState())
						.ratePercent(row.ratePercent())
						.build())
				.toList();
	}
}
