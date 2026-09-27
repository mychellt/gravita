package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateCustomerCommand(
		UUID customerId,
		Document document,
		String name,
		String email,
		IeIndicator ieIndicator,
		Boolean finalConsumer,
		BigDecimal creditLimit,
		List<AddressDomain> addresses,
		List<ContactDomain> contacts,
		CustomerStatus status,
		BigDecimal currentBalance,
		List<CustomerPriceTableLink> priceTables) {
}
