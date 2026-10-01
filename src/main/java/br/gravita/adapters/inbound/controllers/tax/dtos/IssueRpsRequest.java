package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record IssueRpsRequest(
		@NotNull UUID providerCompanyId,
		@NotBlank String providerMunicipalityIbgeCode,
		@NotNull @Valid TomadorRequest tomador,
		@NotBlank String serviceCode,
		@NotNull PlaceOfProvision placeOfProvision,
		@NotNull @Positive BigDecimal serviceAmount,
		@NotBlank String discrimination,
		BigDecimal issRateOverride,
		String overrideJustification) {

	public IssueRpsCommand toCommand() {
		return new IssueRpsCommand(providerCompanyId, providerMunicipalityIbgeCode, tomador.toCommand(), serviceCode,
				placeOfProvision, serviceAmount, discrimination, issRateOverride, overrideJustification);
	}

	public record TomadorRequest(UUID personId, @NotBlank String document, @NotNull PersonType personType,
			@NotBlank String name, String municipalityIbgeCode, AddressRequest address) {

		IssueRpsCommand.TomadorCommand toCommand() {
			return new IssueRpsCommand.TomadorCommand(personId, document, personType, name, municipalityIbgeCode,
					address == null ? null : address.toCommand());
		}
	}

	public record AddressRequest(String street, String number, String complement, String neighborhood,
			String zipCode, String state) {

		IssueRpsCommand.AddressCommand toCommand() {
			return new IssueRpsCommand.AddressCommand(street, number, complement, neighborhood, zipCode, state);
		}
	}
}
