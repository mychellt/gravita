package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;

public record BankAccount(String bankCode, String agency, String accountNumber) {

	public BankAccount {
		requireNonBlank(bankCode, "bank code");
		requireNonBlank(agency, "agency");
		requireNonBlank(accountNumber, "account number");
	}

	private static void requireNonBlank(final String value, final String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Bank account " + field + " is required");
		}
	}
}
