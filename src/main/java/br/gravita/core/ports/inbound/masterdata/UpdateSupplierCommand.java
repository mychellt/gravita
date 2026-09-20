package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.BankAccount;
import br.gravita.core.domain.masterdata.Contact;
import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import java.util.List;

/**
 * Partial update: any field left {@code null} keeps the supplier's current
 * value instead of being cleared, so callers only submit what changed.
 */
public record UpdateSupplierCommand(
		SupplierId supplierId,
		Document document,
		String name,
		List<Address> addresses,
		List<Contact> contacts,
		BankAccount bankAccount,
		PixKey pixKey,
		Integer averageLeadTimeDays,
		String defaultPurchaseCfop) {
}
