package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.adapters.outbound.persistence.mappers.CustomerPersistenceMapperImpl;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({CustomerRepositoryAdapter.class, CustomerPersistenceMapperImpl.class})
class CustomerRepositoryAdapterTest {

	@Autowired
	private CustomerRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Saves a customer with its addresses and retrieves it intact")
	void shouldSaveAndRetrieveCustomerWithAddresses() {
		CustomerDomain customer = customer("Maria Silva", "111.444.777-35");

		CustomerDomain saved = repositoryAdapter.save(customer);

		assertThat(repositoryAdapter.get(saved.getId()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getName()).isEqualTo("Maria Silva");
					assertThat(found.getDocumentDomain().number()).isEqualTo("11144477735");
					assertThat(found.getStatus()).isEqualTo(CustomerStatus.REGULAR);
					assertThat(found.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
					assertThat(found.getAddresses()).hasSize(1);
					assertThat(found.getAddresses().get(0).getCity()).isEqualTo("São Paulo");
					assertThat(found.getAddresses().get(0).isDefault()).isTrue();
				});
	}

	@Test
	@DisplayName("Rejects a stale update instead of silently overwriting a concurrent write")
	void shouldRejectStaleUpdateInsteadOfSilentlyOverwritingAConcurrentWrite() {
		CustomerDomain saved = repositoryAdapter.save(customer("Maria Silva", "111.444.777-35"));
		entityManager.flush();
		entityManager.clear();

		CustomerDomain firstReader = repositoryAdapter.get(saved.getId()).orElseThrow();
		CustomerDomain secondReader = repositoryAdapter.get(saved.getId()).orElseThrow();

		firstReader.setCurrentBalance(new BigDecimal("500.00"));
		repositoryAdapter.save(firstReader);
		entityManager.flush();
		entityManager.clear();

		secondReader.setEmail("new-email@example.com");
		assertThatThrownBy(() -> repositoryAdapter.save(secondReader))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	@DisplayName("Lists all saved customers")
	void shouldListAllCustomers() {
		repositoryAdapter.save(customer("Ana", "111.444.777-35"));
		repositoryAdapter.save(customer("Bruno", "529.982.247-25"));

		assertThat(repositoryAdapter.findAll()).extracting(CustomerDomain::getName).contains("Ana", "Bruno");
	}

	private CustomerDomain customer(String name, String cpf) {
		CustomerDomain customer = CustomerDomain.builder()
				.name(name)
				.documentDomain(Document.cpf(cpf))
				.creditLimit(BigDecimal.ZERO)
				.currentBalance(BigDecimal.ZERO)
				.status(CustomerStatus.REGULAR)
				.addresses(List.of(AddressDomain.builder()
						.type(AddressType.BILLING)
						.street("Rua A")
						.neighborhood("Centro")
						.city("São Paulo")
						.state("SP")
						.zipCode("01000-000")
						.isDefault(true)
						.build()))
				.contacts(List.of())
				.priceTables(List.of())
				.build();
		customer.setId(UUID.randomUUID());
		return customer;
	}
}
