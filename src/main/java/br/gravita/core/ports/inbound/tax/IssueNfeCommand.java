package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.TransportModality;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UC-M2-01. {@code cfop} is deliberately not a field here: AC2 requires the
 * CFOP to be resolved from the free CFOP registry by
 * {@link #naturezaOperacao()}, not accepted as caller input, so there is no
 * second, potentially-hardcoded source of truth for it (see
 * {@link br.gravita.core.ports.outbound.tax.CfopRegistryPort}). Similarly,
 * {@code priceTableId}/{@code discountOverrideJustification} and
 * {@code taxOverrides} aren't in the ticket's original field list but are
 * required to make AC4/AC5 concretely enforceable.
 */
public record IssueNfeCommand(
		UUID issuerCompanyId,
		UUID originSalesOrderId,
		NaturezaOperacao naturezaOperacao,
		RecipientCommand recipient,
		List<ItemCommand> items,
		UUID priceTableId,
		String discountOverrideJustification,
		List<TaxOverrideCommand> taxOverrides,
		BigDecimal freight,
		BigDecimal insurance,
		BigDecimal otherExpenses,
		TransportCommand transport,
		String referencedAccessKey,
		String additionalInfo) {

	public IssueNfeCommand {
		Objects.requireNonNull(issuerCompanyId, "issuerCompanyId");
		Objects.requireNonNull(naturezaOperacao, "naturezaOperacao");
		Objects.requireNonNull(recipient, "recipient");
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("items must not be empty");
		}
		items = List.copyOf(items);
		taxOverrides = taxOverrides == null ? List.of() : List.copyOf(taxOverrides);
	}

	public record RecipientCommand(UUID personId, String document, PersonType personType, String name,
			String stateRegistration, String state) {
	}

	public record ItemCommand(UUID productId, String description, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal discount) {
	}

	public record TransportCommand(TransportModality modality, String carrier, Integer volume,
			BigDecimal grossWeight, BigDecimal netWeight, String rntrc) {
	}
}
