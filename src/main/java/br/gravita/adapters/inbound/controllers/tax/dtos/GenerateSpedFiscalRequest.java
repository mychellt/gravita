package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Accountant;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.ActivityProfile;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.ActivityType;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Finality;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Period;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Taxpayer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * The period is either {@code period} ({@code yyyy-MM}) or {@code startDate} and {@code endDate}. What the
 * identification records need and the company registry does not hold travels in {@code taxpayer} and
 * {@code accountant}; a field left out there is reported as a missing record, not rejected here.
 */
public record GenerateSpedFiscalRequest(@NotNull UUID companyId, YearMonth period, LocalDate startDate,
		LocalDate endDate, Finality finality, @NotNull @Valid TaxpayerRequest taxpayer,
		@NotNull @Valid AccountantRequest accountant) {

	public record TaxpayerRequest(String legalName, String municipalityCode, ActivityProfile profile,
			ActivityType activity, String tradeName, String zipCode, String number, String neighborhood) {
	}

	public record AccountantRequest(String name, String cpf, String crc, String email) {
	}

	@AssertTrue(message = "inform either period (yyyy-MM) or both startDate and endDate")
	public boolean isPeriodInformed() {
		final boolean month = period != null;
		final boolean someDates = startDate != null || endDate != null;
		final boolean bothDates = startDate != null && endDate != null;
		return month ? !someDates : bothDates;
	}

	@AssertTrue(message = "endDate must not be before startDate")
	public boolean isRangeOrdered() {
		return startDate == null || endDate == null || !endDate.isBefore(startDate);
	}

	public GenerateSpedFiscalCommand toCommand() {
		final Period resolved = period != null ? Period.ofMonth(period) : new Period(startDate, endDate);
		return new GenerateSpedFiscalCommand(CompanyId.of(companyId), resolved, finality,
				new Taxpayer(taxpayer.legalName(), taxpayer.municipalityCode(), taxpayer.profile(),
						taxpayer.activity(), taxpayer.tradeName(), taxpayer.zipCode(), taxpayer.number(),
						taxpayer.neighborhood()),
				new Accountant(accountant.name(), accountant.cpf(), accountant.crc(), accountant.email()));
	}
}
