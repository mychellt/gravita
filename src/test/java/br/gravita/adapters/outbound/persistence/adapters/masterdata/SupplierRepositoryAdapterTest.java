package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.masterdata.SupplierPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.BankAccount;
import br.gravita.core.domain.masterdata.Contact;
import br.gravita.core.domain.masterdata.ContactType;
import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({SupplierRepositoryAdapter.class, SupplierPersistenceMapperImpl.class})
class SupplierRepositoryAdapterTest {

	private static final Address VALID_ADDRESS =
			new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000");

	@Autowired
	private SupplierRepositoryAdapter repositoryAdapter;

	@Test
	void shouldPersistNewSupplierWithApplicationAssignedId() {
		Supplier supplier = newSupplier(SupplierId.of(UUID.randomUUID()));

		Supplier saved = repositoryAdapter.save(supplier);

		assertThat(repositoryAdapter.findById(saved.getId())).isPresent().get()
				.satisfies(found -> {
					assertThat(found.getDocument()).isEqualTo(supplier.getDocument());
					assertThat(found.getAddresses()).containsExactly(VALID_ADDRESS);
					assertThat(found.getBankAccount()).isNull();
					assertThat(found.getPixKey()).isNull();
				});
	}

	@Test
	void shouldPersistOptionalBankAccountPixKeyAndPurchasingFields() {
		SupplierId id = SupplierId.of(UUID.randomUUID());
		Supplier supplier = Supplier.of(id, Document.cnpj("11222333000181"), "Acme Supplies", List.of(VALID_ADDRESS),
				List.of(new Contact(ContactType.EMAIL, "purchasing@acme.com")),
				new BankAccount("001", "1234", "56789-0"), PixKey.of("supplier@example.com"), 5, "1102");

		repositoryAdapter.save(supplier);

		assertThat(repositoryAdapter.findById(id)).isPresent().get()
				.satisfies(found -> {
					assertThat(found.getBankAccount()).isEqualTo(new BankAccount("001", "1234", "56789-0"));
					assertThat(found.getPixKey()).isEqualTo(PixKey.of("supplier@example.com"));
					assertThat(found.getAverageLeadTimeDays()).isEqualTo(5);
					assertThat(found.getDefaultPurchaseCfop()).isEqualTo("1102");
				});
	}

	@Test
	void shouldUpdateExistingSupplierWithoutLosingCreatedAt() {
		Supplier supplier = newSupplier(SupplierId.of(UUID.randomUUID()));
		Supplier saved = repositoryAdapter.save(supplier);

		Supplier changed = Supplier.of(saved.getId(), saved.getDocument(), "New Name", saved.getAddresses(),
				saved.getContacts(), saved.getBankAccount(), saved.getPixKey(), 10, "2102");

		repositoryAdapter.save(changed);

		assertThat(repositoryAdapter.findById(saved.getId())).isPresent().get()
				.satisfies(found -> {
					assertThat(found.getName()).isEqualTo("New Name");
					assertThat(found.getAverageLeadTimeDays()).isEqualTo(10);
				});
	}

	private Supplier newSupplier(SupplierId id) {
		return Supplier.of(id, Document.cnpj("11222333000181"), "Acme Supplies", List.of(VALID_ADDRESS), List.of(),
				null, null, null, null);
	}
}
