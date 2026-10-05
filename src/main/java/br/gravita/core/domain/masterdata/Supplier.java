package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public final class Supplier {

	private static final Pattern CFOP = Pattern.compile("\\d{4}");

	private final SupplierId id;
	private final Document document;
	private final String name;
	private final List<Address> addresses;
	private final List<Contact> contacts;
	private final BankAccount bankAccount;
	private final PixKey pixKey;
	private final Integer averageLeadTimeDays;
	private final String defaultPurchaseCfop;

	public Supplier(final SupplierId id, final Document document, final String name, final List<Address> addresses, final List<Contact> contacts,
			final BankAccount bankAccount, final PixKey pixKey, final Integer averageLeadTimeDays, final String defaultPurchaseCfop) {
		this.id = Objects.requireNonNull(id, "SupplierId is required");
		this.document = requireDocument(document);
		this.name = requireName(name);
		this.addresses = requireAddresses(addresses);
		this.contacts = contacts == null ? List.of() : List.copyOf(contacts);
		this.bankAccount = bankAccount;
		this.pixKey = pixKey;
		this.averageLeadTimeDays = requireValidLeadTime(averageLeadTimeDays);
		this.defaultPurchaseCfop = requireValidCfop(defaultPurchaseCfop);
	}

	public static Supplier of(final SupplierId id, final Document document, final String name, final List<Address> addresses,
			final List<Contact> contacts, final BankAccount bankAccount, final PixKey pixKey, final Integer averageLeadTimeDays,
			final String defaultPurchaseCfop) {
		return new Supplier(id, document, name, addresses, contacts, bankAccount, pixKey, averageLeadTimeDays,
				defaultPurchaseCfop);
	}

	public PersonType personType() {
		return document.personType();
	}

	public void assertReadyForPurchasing() {
		if (averageLeadTimeDays == null) {
			throw new BusinessRuleException("Average lead time is required before using this supplier in a purchase order");
		}
		if (defaultPurchaseCfop == null) {
			throw new BusinessRuleException("Default purchase CFOP is required before using this supplier in a purchase order");
		}
	}

	private static Document requireDocument(final Document document) {
		if (document == null) {
			throw new BusinessRuleException("Supplier document is required");
		}
		return document;
	}

	private static String requireName(final String name) {
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Supplier name is required");
		}
		return name;
	}

	private static List<Address> requireAddresses(final List<Address> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			throw new BusinessRuleException("At least one address is required");
		}
		return List.copyOf(addresses);
	}

	private static Integer requireValidLeadTime(final Integer averageLeadTimeDays) {
		if (averageLeadTimeDays == null) {
			return null;
		}
		if (averageLeadTimeDays <= 0) {
			throw new BusinessRuleException("Average lead time must be a positive number of days: " + averageLeadTimeDays);
		}
		return averageLeadTimeDays;
	}

	private static String requireValidCfop(final String defaultPurchaseCfop) {
		if (defaultPurchaseCfop == null || defaultPurchaseCfop.isBlank()) {
			return null;
		}
		final String trimmed = defaultPurchaseCfop.trim();
		if (!CFOP.matcher(trimmed).matches()) {
			throw new BusinessRuleException("Invalid CFOP: " + defaultPurchaseCfop);
		}
		return trimmed;
	}
}
