package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CustomerDomain extends AbstractDomain{
	private String name;
	private DocumentDomain documentDomain;
	private String email;
	private CustomerStatus status;

	public void block() {
		if (status == CustomerStatus.BLOCKED) {
			throw new BusinessRuleException("Customer is already blocked");
		}
		this.status = CustomerStatus.BLOCKED;
	}

	public void reactivate() {
		this.status = CustomerStatus.ACTIVE;
	}

	private static String validateName(String name) {
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Customer name is required");
		}
		return name.trim();
	}
}
