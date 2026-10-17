package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.BankAccount;
import br.gravita.core.domain.masterdata.Contact;
import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.shared.Document;
import java.util.List;

public record RegisterSupplierCommand(
		Document document,
		String name,
		List<Address> addresses,
		List<Contact> contacts,
		BankAccount bankAccount,
		PixKey pixKey,
		Integer averageLeadTimeDays,
		String defaultPurchaseCfop,
		String ie,
		String im,
		String cnae,
		Boolean simplesOptante) {

	/** A command without the optional fiscal profile. */
	public RegisterSupplierCommand(final Document document,
			final String name,
			final List<Address> addresses,
			final List<Contact> contacts,
			final BankAccount bankAccount,
			final PixKey pixKey,
			final Integer averageLeadTimeDays,
			final String defaultPurchaseCfop) {
		this(document, name, addresses, contacts, bankAccount, pixKey, averageLeadTimeDays, defaultPurchaseCfop, null, null, null, null);
	}
}
