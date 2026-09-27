package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransportModality;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.TaxOverrideCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IssueNfeRequest(
		@NotNull UUID issuerCompanyId,
		UUID originSalesOrderId,
		@NotNull NaturezaOperacao naturezaOperacao,
		@NotNull @Valid RecipientRequest recipient,
		@NotEmpty List<@Valid ItemRequest> items,
		UUID priceTableId,
		String discountOverrideJustification,
		List<@Valid TaxOverrideRequest> taxOverrides,
		BigDecimal freight,
		BigDecimal insurance,
		BigDecimal otherExpenses,
		@Valid TransportRequest transport,
		String referencedAccessKey,
		String additionalInfo) {

	public IssueNfeCommand toCommand() {
		return new IssueNfeCommand(issuerCompanyId, originSalesOrderId, naturezaOperacao, recipient.toCommand(),
				items.stream().map(ItemRequest::toCommand).toList(), priceTableId, discountOverrideJustification,
				taxOverrides == null ? List.of() : taxOverrides.stream().map(TaxOverrideRequest::toCommand).toList(),
				freight, insurance, otherExpenses, transport == null ? null : transport.toCommand(),
				referencedAccessKey, additionalInfo);
	}

	public record RecipientRequest(UUID personId, @NotNull String document, @NotNull PersonType personType,
			@NotNull String name, String stateRegistration, @NotNull String state) {

		IssueNfeCommand.RecipientCommand toCommand() {
			return new IssueNfeCommand.RecipientCommand(personId, document, personType, name, stateRegistration,
					state);
		}
	}

	public record ItemRequest(@NotNull UUID productId, String description, @NotNull BigDecimal quantity,
			@NotNull BigDecimal unitPrice, BigDecimal discount) {

		IssueNfeCommand.ItemCommand toCommand() {
			return new IssueNfeCommand.ItemCommand(productId, description, quantity, unitPrice, discount);
		}
	}

	public record TaxOverrideRequest(int itemIndex, @NotNull TaxType tax, @NotNull BigDecimal value,
			String justification) {

		TaxOverrideCommand toCommand() {
			return new TaxOverrideCommand(itemIndex, tax, value, justification);
		}
	}

	public record TransportRequest(TransportModality modality, String carrier, Integer volume,
			BigDecimal grossWeight, BigDecimal netWeight, String rntrc) {

		IssueNfeCommand.TransportCommand toCommand() {
			return new IssueNfeCommand.TransportCommand(modality, carrier, volume, grossWeight, netWeight, rntrc);
		}
	}
}
