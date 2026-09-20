package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CustomerDomain extends AbstractDomain {
	private String name;
	private Document documentDomain;
	private String email;
	private IeIndicator ieIndicator;
	private Boolean finalConsumer;
	private BigDecimal creditLimit;
	private BigDecimal currentBalance;
	private CustomerStatus status;
	private Long version;
	private List<AddressDomain> addresses;
	private List<ContactDomain> contacts;
	private List<CustomerPriceTableLink> priceTables;

	public void block() {
		if (status == CustomerStatus.BLOCKED) {
			throw new BusinessRuleException("Customer is already blocked");
		}
		this.status = CustomerStatus.BLOCKED;
	}

	public void reactivate() {
		this.status = CustomerStatus.REGULAR;
	}

	public void applyCreditStatus(BigDecimal currentBalance, CustomerStatus status) {
		if (currentBalance == null) {
			throw new BusinessRuleException("Current balance is required");
		}
		if (status == null) {
			throw new BusinessRuleException("Status is required");
		}
		this.currentBalance = currentBalance;
		this.status = status;
	}

	public void validateForRegistration() {
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Customer name is required");
		}
		if (documentDomain == null) {
			throw new BusinessRuleException("Customer document is required");
		}
		if (documentDomain.personType() == PersonType.COMPANY) {
			if (ieIndicator == null) {
				throw new BusinessRuleException("IE indicator is required for a PJ customer");
			}
			if (finalConsumer == null) {
				throw new BusinessRuleException("Final consumer flag is required for a PJ customer");
			}
		}
		validateAddresses();
		validatePriceTables();
	}

	private void validateAddresses() {
		if (addresses == null || addresses.isEmpty()) {
			throw new BusinessRuleException("At least one address is required");
		}
		for (AddressType type : AddressType.values()) {
			List<AddressDomain> ofType = addresses.stream().filter(address -> address.getType() == type).toList();
			if (ofType.isEmpty()) {
				continue;
			}
			long defaults = ofType.stream().filter(AddressDomain::isDefault).count();
			if (defaults != 1) {
				throw new BusinessRuleException("Each address type must have exactly one default address: " + type);
			}
		}
	}

	private void validatePriceTables() {
		if (priceTables == null || priceTables.isEmpty()) {
			return;
		}
		Set<Integer> priorities = new HashSet<>();
		for (CustomerPriceTableLink link : priceTables) {
			if (link.getPriority() == null) {
				throw new BusinessRuleException("Price table priority is required");
			}
			if (!priorities.add(link.getPriority())) {
				throw new BusinessRuleException("Price table priority must be unique: " + link.getPriority());
			}
		}
	}
}
