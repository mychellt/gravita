package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.PersonType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
		UUID id,
		String name,
		PersonType type,
		String document,
		String email,
		IeIndicator ieIndicator,
		Boolean finalConsumer,
		BigDecimal creditLimit,
		BigDecimal currentBalance,
		CustomerStatus status,
		List<AddressResponse> addresses,
		List<ContactResponse> contacts,
		List<PriceTableLinkResponse> priceTables) {

	public static CustomerResponse from(CustomerDomain domain) {
		return new CustomerResponse(
				domain.getId(),
				domain.getName(),
				domain.getDocumentDomain().personType(),
				domain.getDocumentDomain().formatted(),
				domain.getEmail(),
				domain.getIeIndicator(),
				domain.getFinalConsumer(),
				domain.getCreditLimit(),
				domain.getCurrentBalance(),
				domain.getStatus(),
				domain.getAddresses() == null ? List.of() : domain.getAddresses().stream().map(AddressResponse::from).toList(),
				domain.getContacts() == null ? List.of() : domain.getContacts().stream().map(ContactResponse::from).toList(),
				domain.getPriceTables() == null ? List.of() : domain.getPriceTables().stream().map(PriceTableLinkResponse::from).toList());
	}

	public record AddressResponse(String type, String street, String number, String complement, String neighborhood,
			String city, String state, String zipCode, boolean isDefault) {

		static AddressResponse from(AddressDomain domain) {
			return new AddressResponse(domain.getType().name(), domain.getStreet(), domain.getNumber(), domain.getComplement(),
					domain.getNeighborhood(), domain.getCity(), domain.getState(), domain.getZipCode(), domain.isDefault());
		}
	}

	public record ContactResponse(String type, String value) {

		static ContactResponse from(ContactDomain domain) {
			return new ContactResponse(domain.getType().name(), domain.getValue());
		}
	}

	public record PriceTableLinkResponse(UUID priceTableId, Integer priority) {

		static PriceTableLinkResponse from(CustomerPriceTableLink domain) {
			return new PriceTableLinkResponse(domain.getPriceTableId(), domain.getPriority());
		}
	}
}
