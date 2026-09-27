package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransportModality;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.NfeItemInput;
import br.gravita.core.ports.inbound.tax.NfeItemTaxOverrideInput;
import br.gravita.core.ports.inbound.tax.NfeRecipientInput;
import br.gravita.core.ports.inbound.tax.NfeTransportInput;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IssueNfeRequest(
		UUID issuerCompanyId,
		UUID originSalesOrderId,
		String naturezaOperacao,
		Recipient recipient,
		List<Item> items,
		BigDecimal freight,
		BigDecimal insurance,
		BigDecimal otherExpenses,
		Transport transport,
		String referencedAccessKey,
		String additionalInfo,
		UUID priceTableId) {

	public record Recipient(UUID customerId, String document, PersonType documentType, String name,
			IeIndicator ieIndicator, String ie, String state) {
	}

	public record Item(UUID productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercent,
			String discountOverrideJustification, List<TaxOverride> taxOverrides) {
	}

	public record TaxOverride(TaxType tax, BigDecimal value, String justification) {
	}

	public record Transport(TransportModality modality, String carrier, String volume, BigDecimal grossWeight,
			BigDecimal netWeight, String rntrc) {
	}

	public IssueNfeCommand toCommand() {
		return new IssueNfeCommand(CompanyId.of(issuerCompanyId), originSalesOrderId, naturezaOperacao,
				toRecipientInput(), toItemInputs(), freight, insurance, otherExpenses, toTransportInput(),
				referencedAccessKey, additionalInfo, priceTableId);
	}

	private NfeRecipientInput toRecipientInput() {
		return new NfeRecipientInput(recipient.customerId(), recipient.document(), recipient.documentType(),
				recipient.name(), recipient.ieIndicator(), recipient.ie(), recipient.state());
	}

	private List<NfeItemInput> toItemInputs() {
		return items.stream()
				.map(item -> new NfeItemInput(item.productId(), item.quantity(), item.unitPrice(),
						item.discountPercent(), item.discountOverrideJustification(), toOverrideInputs(item.taxOverrides())))
				.toList();
	}

	private List<NfeItemTaxOverrideInput> toOverrideInputs(List<TaxOverride> overrides) {
		if (overrides == null) {
			return List.of();
		}
		return overrides.stream()
				.map(override -> new NfeItemTaxOverrideInput(override.tax(), override.value(), override.justification()))
				.toList();
	}

	private NfeTransportInput toTransportInput() {
		if (transport == null) {
			return null;
		}
		return new NfeTransportInput(transport.modality(), transport.carrier(), transport.volume(),
				transport.grossWeight(), transport.netWeight(), transport.rntrc());
	}
}
