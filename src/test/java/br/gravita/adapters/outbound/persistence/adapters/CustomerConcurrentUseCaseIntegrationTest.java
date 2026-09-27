package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.mappers.CustomerPersistenceMapperImpl;
import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerCommand;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.usercases.SetCustomerCreditStatusAdapter;
import br.gravita.core.usercases.UpdateCustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({CustomerRepositoryAdapter.class, CustomerPersistenceMapperImpl.class})
class CustomerConcurrentUseCaseIntegrationTest {

	@Autowired
	private CustomerRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void patchShouldRejectStaleWriteWhenCreditStatusChangeWonTheRace() {
		UUID id = repositoryAdapter.save(customer()).getId();
		entityManager.flush();
		entityManager.clear();

		CustomerRepositoryPort staleReadPort = staleSnapshotPortFor(id);
		UpdateCustomerService updateCustomerService = new UpdateCustomerService(staleReadPort);

		SetCustomerCreditStatusAdapter creditStatusAdapter = new SetCustomerCreditStatusAdapter(repositoryAdapter);
		CustomerDomain creditCommand = CustomerDomain.builder()
				.id(id)
				.currentBalance(new BigDecimal("500.00"))
				.status(CustomerStatus.BLOCKED)
				.build();
		creditStatusAdapter.execute(new Context(creditCommand));
		entityManager.flush();
		entityManager.clear();

		UpdateCustomerCommand patchCommand = new UpdateCustomerCommand(
				id, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> updateCustomerService.execute(patchCommand))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		CustomerDomain persisted = repositoryAdapter.get(id).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
		assertThat(persisted.getCurrentBalance()).isEqualByComparingTo("500.00");
		assertThat(persisted.getName()).isEqualTo("Maria Silva");
	}

	@Test
	void creditStatusChangeShouldRejectStaleWriteWhenPatchWonTheRace() {
		UUID id = repositoryAdapter.save(customer()).getId();
		entityManager.flush();
		entityManager.clear();

		CustomerRepositoryPort staleReadPort = staleSnapshotPortFor(id);
		SetCustomerCreditStatusAdapter creditStatusAdapter = new SetCustomerCreditStatusAdapter(staleReadPort);

		UpdateCustomerService updateCustomerService = new UpdateCustomerService(repositoryAdapter);
		UpdateCustomerCommand patchCommand = new UpdateCustomerCommand(
				id, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);
		updateCustomerService.execute(patchCommand);
		entityManager.flush();
		entityManager.clear();

		CustomerDomain creditCommand = CustomerDomain.builder()
				.id(id)
				.currentBalance(new BigDecimal("500.00"))
				.status(CustomerStatus.BLOCKED)
				.build();

		assertThatThrownBy(() -> creditStatusAdapter.execute(new Context(creditCommand)))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		CustomerDomain persisted = repositoryAdapter.get(id).orElseThrow();
		assertThat(persisted.getName()).isEqualTo("Maria S. Costa");
		assertThat(persisted.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(persisted.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void normalSingleWriterPatchLeavesStatusAndBalanceUntouchedWhenOmitted() {
		UUID id = repositoryAdapter.save(customer()).getId();
		entityManager.flush();
		entityManager.clear();

		UpdateCustomerService updateCustomerService = new UpdateCustomerService(repositoryAdapter);
		UpdateCustomerCommand patchCommand = new UpdateCustomerCommand(
				id, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);

		updateCustomerService.execute(patchCommand);
		entityManager.flush();
		entityManager.clear();

		CustomerDomain persisted = repositoryAdapter.get(id).orElseThrow();
		assertThat(persisted.getName()).isEqualTo("Maria S. Costa");
		assertThat(persisted.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(persisted.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void patchWithNewValidDocumentIsPersistedAndFormattedCorrectlyOnRead() {
		UUID id = repositoryAdapter.save(customer()).getId();
		entityManager.flush();
		entityManager.clear();

		UpdateCustomerService updateCustomerService = new UpdateCustomerService(repositoryAdapter);
		UpdateCustomerCommand patchCommand = new UpdateCustomerCommand(
				id, Document.cpf("529.982.247-25"), null, null, null, null, null, null, null, null, null, null);

		updateCustomerService.execute(patchCommand);
		entityManager.flush();
		entityManager.clear();

		CustomerDomain persisted = repositoryAdapter.get(id).orElseThrow();
		assertThat(persisted.getDocumentDomain().number()).isEqualTo("52998224725");
		assertThat(persisted.getDocumentDomain().formatted()).isEqualTo("529.982.247-25");
	}

	private CustomerRepositoryPort staleSnapshotPortFor(UUID id) {
		CustomerDomain snapshot = repositoryAdapter.get(id).orElseThrow();
		return new CustomerRepositoryPort() {
			@Override
			public Optional<CustomerDomain> get(UUID lookupId) {
				return Optional.of(snapshot);
			}

			@Override
			public List<CustomerDomain> findAll() {
				return repositoryAdapter.findAll();
			}

			@Override
			public CustomerDomain save(CustomerDomain model) {
				return repositoryAdapter.save(model);
			}
		};
	}

	private CustomerDomain customer() {
		CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
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
