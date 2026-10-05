package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.ContactType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression: with open-in-view off, reading a customer that has addresses, contacts or price tables failed with
 * a LazyInitializationException (HTTP 500 on {@code GET /api/customers} and {@code /api/customers/{id}}), because
 * the adapter mapped the entity to the domain after its transaction had ended. Runs against the real database.
 */
@SpringBootTest
class CustomerRepositoryAdapterLazyLoadingIntegrationTest {

	@Autowired
	private CustomerRepositoryPort customers;
	@Autowired
	private CompanyRepositoryPort companies;
	@Autowired
	private JdbcTemplate jdbc;

	private UUID customerId;
	private UUID companyId;

	@AfterEach
	void cleanUp() {
		if (customerId != null) {
			jdbc.update("delete from customer_addresses where customer_id = ?", customerId);
			jdbc.update("delete from customer_contacts where customer_id = ?", customerId);
			jdbc.update("delete from customers where id = ?", customerId);
		}
		if (companyId != null) {
			jdbc.update("delete from companies where id = ?", companyId);
		}
	}

	/** A CNPJ with valid check digits, different on every run, so the test never clashes with existing rows. */
	private static String newCnpj() {
		final int[] digits = new int[14];
		for (int i = 0; i < 12; i++) {
			digits[i] = java.util.concurrent.ThreadLocalRandom.current().nextInt(10);
		}
		digits[0] = 1 + digits[0] % 9;
		digits[12] = checkDigit(digits, 12, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
		digits[13] = checkDigit(digits, 13, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
		final StringBuilder cnpj = new StringBuilder();
		for (final int digit : digits) {
			cnpj.append(digit);
		}
		return cnpj.toString();
	}

	private static int checkDigit(final int[] digits, final int length, final int[] weights) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += digits[i] * weights[i];
		}
		final int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}

	private CustomerDomain saveCustomerWithChildren() {
		companyId = UUID.randomUUID();
		companies.save(Company.draft(CompanyId.of(companyId), "Tenant", Document.cnpj(newCnpj()), null));
		customerId = UUID.randomUUID();
		return customers.save(CustomerDomain.builder()
				.id(customerId).name("Cliente").documentDomain(Document.cnpj(newCnpj()))
				.ieIndicator(IeIndicator.TAXPAYER).finalConsumer(false)
				.creditLimit(BigDecimal.TEN).currentBalance(BigDecimal.ZERO).status(CustomerStatus.REGULAR)
				.companyId(companyId)
				.addresses(List.of(AddressDomain.builder().type(AddressType.BILLING).street("Rua A").neighborhood("Centro")
						.city("São Paulo").state("SP").zipCode("01310100").isDefault(true).build()))
				.contacts(List.of(ContactDomain.builder().type(ContactType.EMAIL).value("a@b.co").build()))
				.priceTables(List.of())
				.build());
	}

	@Test
	@DisplayName("Reads a customer's addresses and contacts by id and by company without a lazy-loading failure")
	void readsLazyCollections() {
		saveCustomerWithChildren();

		final var byId = customers.get(customerId).orElseThrow();
		assertThat(byId.getAddresses()).hasSize(1);
		assertThat(byId.getAddresses().get(0).getStreet()).isEqualTo("Rua A");
		assertThat(byId.getContacts()).extracting(ContactDomain::getValue).containsExactly("a@b.co");

		final var scoped = customers.findByIdAndCompanyId(customerId, companyId).orElseThrow();
		assertThat(scoped.getAddresses()).hasSize(1);

		final var all = customers.findAllByCompanyId(companyId);
		assertThat(all).hasSize(1);
		assertThat(all.get(0).getAddresses()).hasSize(1);
		assertThat(customers.findAll()).anyMatch(c -> customerId.equals(c.getId()) && c.getAddresses().size() == 1);
	}
}
