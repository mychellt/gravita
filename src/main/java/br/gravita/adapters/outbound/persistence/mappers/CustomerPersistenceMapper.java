package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.CustomerAddressEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.CustomerContactEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.CustomerPriceTableEmbeddable;
import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.shared.Document;

import java.util.List;

class CustomerPersistenceMapper {

	CustomerDomain toDomain(CustomerJpaEntity entity) {
		return CustomerDomain.builder()
				.id(entity.getId())
				.name(entity.getName())
				.documentDomain(new Document(entity.getDocument(), entity.getPersonType()))
				.email(entity.getEmail())
				.ieIndicator(entity.getIeIndicator())
				.finalConsumer(entity.getFinalConsumer())
				.creditLimit(entity.getCreditLimit())
				.currentBalance(entity.getCurrentBalance())
				.status(entity.getStatus())
				.addresses(entity.getAddresses() == null ? List.of() : entity.getAddresses().stream().map(this::toDomain).toList())
				.contacts(entity.getContacts() == null ? List.of() : entity.getContacts().stream().map(this::toDomain).toList())
				.priceTables(entity.getPriceTables() == null ? List.of() : entity.getPriceTables().stream().map(this::toDomain).toList())
				.build();
	}

	CustomerJpaEntity toEntity(CustomerDomain domain) {
		return CustomerJpaEntity.builder()
				.id(domain.getId())
				.name(domain.getName())
				.personType(domain.getDocumentDomain().personType())
				.document(domain.getDocumentDomain().number())
				.email(domain.getEmail())
				.ieIndicator(domain.getIeIndicator())
				.finalConsumer(domain.getFinalConsumer())
				.creditLimit(domain.getCreditLimit())
				.currentBalance(domain.getCurrentBalance())
				.status(domain.getStatus())
				.addresses(toAddressEmbeddables(domain.getAddresses()))
				.contacts(toContactEmbeddables(domain.getContacts()))
				.priceTables(toPriceTableEmbeddables(domain.getPriceTables()))
				.build();
	}

	private AddressDomain toDomain(CustomerAddressEmbeddable embeddable) {
		return AddressDomain.builder()
				.type(embeddable.getType())
				.street(embeddable.getStreet())
				.number(embeddable.getNumber())
				.complement(embeddable.getComplement())
				.neighborhood(embeddable.getNeighborhood())
				.city(embeddable.getCity())
				.state(embeddable.getState())
				.zipCode(embeddable.getZipCode())
				.isDefault(embeddable.isDefault())
				.build();
	}

	private ContactDomain toDomain(CustomerContactEmbeddable embeddable) {
		return ContactDomain.builder().type(embeddable.getType()).value(embeddable.getValue()).build();
	}

	private CustomerPriceTableLink toDomain(CustomerPriceTableEmbeddable embeddable) {
		return CustomerPriceTableLink.builder().priceTableId(embeddable.getPriceTableId()).priority(embeddable.getPriority()).build();
	}

	private List<CustomerAddressEmbeddable> toAddressEmbeddables(List<AddressDomain> addresses) {
		if (addresses == null) {
			return List.of();
		}
		return addresses.stream()
				.map(address -> CustomerAddressEmbeddable.builder()
						.type(address.getType())
						.street(address.getStreet())
						.number(address.getNumber())
						.complement(address.getComplement())
						.neighborhood(address.getNeighborhood())
						.city(address.getCity())
						.state(address.getState())
						.zipCode(address.getZipCode())
						.isDefault(address.isDefault())
						.build())
				.toList();
	}

	private List<CustomerContactEmbeddable> toContactEmbeddables(List<ContactDomain> contacts) {
		if (contacts == null) {
			return List.of();
		}
		return contacts.stream()
				.map(contact -> CustomerContactEmbeddable.builder().type(contact.getType()).value(contact.getValue()).build())
				.toList();
	}

	private List<CustomerPriceTableEmbeddable> toPriceTableEmbeddables(List<CustomerPriceTableLink> priceTables) {
		if (priceTables == null) {
			return List.of();
		}
		return priceTables.stream()
				.map(link -> CustomerPriceTableEmbeddable.builder().priceTableId(link.getPriceTableId()).priority(link.getPriority()).build())
				.toList();
	}
}
