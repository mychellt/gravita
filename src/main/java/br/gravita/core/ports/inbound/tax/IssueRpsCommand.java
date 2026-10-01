package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.PlaceOfProvision;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code provider} is the issuing company plus the IBGE code of its municipality (the company master data only keeps
 * a free-text address and UF); {@code serviceAmount} is the value the ISS rate and withholdings are applied to.
 * {@code issRateOverride} (a percentage) is only accepted together with an {@code overrideJustification}.
 */
public record IssueRpsCommand(UUID providerCompanyId, String providerMunicipalityIbgeCode, TomadorCommand tomador,
		String serviceCode, PlaceOfProvision placeOfProvision, BigDecimal serviceAmount, String discrimination,
		BigDecimal issRateOverride, String overrideJustification) {

	public IssueRpsCommand {
		Objects.requireNonNull(providerCompanyId, "providerCompanyId is required");
		Objects.requireNonNull(providerMunicipalityIbgeCode, "providerMunicipalityIbgeCode is required");
		Objects.requireNonNull(tomador, "tomador is required");
		Objects.requireNonNull(serviceCode, "serviceCode is required");
		Objects.requireNonNull(placeOfProvision, "placeOfProvision is required");
		Objects.requireNonNull(serviceAmount, "serviceAmount is required");
		Objects.requireNonNull(discrimination, "discrimination is required");
	}

	/** {@code personId} is set for an already registered customer and {@code null} for a one-off tomador. */
	public record TomadorCommand(UUID personId, String document, PersonType personType, String name,
			String municipalityIbgeCode, AddressCommand address) {

		public TomadorCommand {
			Objects.requireNonNull(document, "tomador document is required");
			Objects.requireNonNull(personType, "tomador personType is required");
			Objects.requireNonNull(name, "tomador name is required");
		}
	}

	public record AddressCommand(String street, String number, String complement, String neighborhood,
			String zipCode, String state) {
	}
}
