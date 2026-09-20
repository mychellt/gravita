package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.ContactType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegisterCustomerRequest(
		@NotNull PersonType type,
		@NotBlank String document,
		@NotBlank String name,
		String email,
		IeIndicator ieIndicator,
		Boolean finalConsumer,
		@PositiveOrZero BigDecimal creditLimit,
		@NotEmpty @Valid List<AddressRequest> addresses,
		@Valid List<ContactRequest> contacts,
		@Valid List<PriceTableLinkRequest> priceTables) {

	public CustomerDomain toDomain() {
		Document documentDomain = type == PersonType.COMPANY ? Document.cnpj(document) : Document.cpf(document);
		return CustomerDomain.builder()
				.name(name)
				.documentDomain(documentDomain)
				.email(email)
				.ieIndicator(ieIndicator)
				.finalConsumer(finalConsumer)
				.creditLimit(creditLimit == null ? BigDecimal.ZERO : creditLimit)
				.addresses(addresses.stream().map(AddressRequest::toDomain).toList())
				.contacts(contacts == null ? List.of() : contacts.stream().map(ContactRequest::toDomain).toList())
				.priceTables(priceTables == null ? List.of() : priceTables.stream().map(PriceTableLinkRequest::toDomain).toList())
				.build();
	}

	public record AddressRequest(
			@NotNull AddressType type,
			@NotBlank String street,
			String number,
			String complement,
			@NotBlank String neighborhood,
			@NotBlank String city,
			@NotBlank String state,
			@NotBlank String zipCode,
			boolean isDefault) {

		AddressDomain toDomain() {
			return AddressDomain.builder()
					.type(type)
					.street(street)
					.number(number)
					.complement(complement)
					.neighborhood(neighborhood)
					.city(city)
					.state(state)
					.zipCode(zipCode)
					.isDefault(isDefault)
					.build();
		}
	}

	public record ContactRequest(@NotNull ContactType type, @NotBlank String value) {

		ContactDomain toDomain() {
			return ContactDomain.builder().type(type).value(value).build();
		}
	}

	public record PriceTableLinkRequest(@NotNull UUID priceTableId, @NotNull @PositiveOrZero Integer priority) {

		CustomerPriceTableLink toDomain() {
			return CustomerPriceTableLink.builder().priceTableId(priceTableId).priority(priority).build();
		}
	}
}
