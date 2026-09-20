package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateCustomerRequest(
		PersonType type,
		String document,
		String name,
		String email,
		IeIndicator ieIndicator,
		Boolean finalConsumer,
		@PositiveOrZero BigDecimal creditLimit,
		@Valid List<RegisterCustomerRequest.AddressRequest> addresses,
		@Valid List<RegisterCustomerRequest.ContactRequest> contacts,
		CustomerStatus status,
		BigDecimal currentBalance,
		@Valid List<RegisterCustomerRequest.PriceTableLinkRequest> priceTables) {

	public UpdateCustomerCommand toCommand(UUID customerId) {
		return new UpdateCustomerCommand(
				customerId,
				toDocument(),
				name,
				email,
				ieIndicator,
				finalConsumer,
				creditLimit,
				addresses == null ? null : addresses.stream().map(RegisterCustomerRequest.AddressRequest::toDomain).toList(),
				contacts == null ? null : contacts.stream().map(RegisterCustomerRequest.ContactRequest::toDomain).toList(),
				status,
				currentBalance,
				priceTables == null ? null : priceTables.stream().map(RegisterCustomerRequest.PriceTableLinkRequest::toDomain).toList());
	}

	private Document toDocument() {
		if (document == null || document.isBlank()) {
			return null;
		}
		if (type == null) {
			throw new BusinessRuleException("type is required when updating the customer document");
		}
		return type == PersonType.COMPANY ? Document.cnpj(document) : Document.cpf(document);
	}
}
