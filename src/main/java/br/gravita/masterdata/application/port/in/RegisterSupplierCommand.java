package br.gravita.masterdata.application.port.in;

import br.gravita.masterdata.domain.model.Address;
import br.gravita.masterdata.domain.model.BankAccount;
import br.gravita.masterdata.domain.model.Contact;
import br.gravita.masterdata.domain.model.PixKey;
import br.gravita.shared.Document;
import java.util.List;

public record RegisterSupplierCommand(
		Document document,
		String name,
		List<Address> addresses,
		List<Contact> contacts,
		BankAccount bankAccount,
		PixKey pixKey,
		Integer averageLeadTimeDays,
		String defaultPurchaseCfop) {
}
