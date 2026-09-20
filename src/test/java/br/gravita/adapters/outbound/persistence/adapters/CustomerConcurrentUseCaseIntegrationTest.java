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

/**
 * GRA-48 acceptance criterion #3: a read-modify-write race between PATCH
 * /api/customers/{id} (UC-06) and a concurrent credit-status change (UC-08)
 * must fail with an optimistic-lock conflict instead of one silently
 * reverting the other's committed change. Drives the real
 * UpdateCustomerService and SetCustomerCreditStatusAdapter production
 * classes - not raw domain mutation - against a real H2-backed
 * CustomerRepositoryAdapter, so the version column is exercised exactly as
 * it would be by two racing use cases sharing the same customer row.
 */
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

		// UC-06's reader: snapshot taken before UC-08 commits its change.
		CustomerRepositoryPort staleReadPort = staleSnapshotPortFor(id);
		UpdateCustomerService updateCustomerService = new UpdateCustomerService(staleReadPort);

		// UC-08 commits first against the real adapter, bumping the row's version.
		SetCustomerCreditStatusAdapter creditStatusAdapter = new SetCustomerCreditStatusAdapter(repositoryAdapter);
		CustomerDomain creditCommand = CustomerDomain.builder()
				.id(id)
				.currentBalance(new BigDecimal("500.00"))
				.status(CustomerStatus.BLOCKED)
				.build();
		creditStatusAdapter.execute(new Context(creditCommand));
		entityManager.flush();
		entityManager.clear();

		// UC-06's PATCH now tries to save against its now-stale snapshot.
		UpdateCustomerCommand patchCommand = new UpdateCustomerCommand(
				id, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> updateCustomerService.execute(patchCommand))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		// UC-08's committed status/balance change must survive untouched.
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

		// UC-08's reader: snapshot taken before the PATCH commits its change.
		CustomerRepositoryPort staleReadPort = staleSnapshotPortFor(id);
		SetCustomerCreditStatusAdapter creditStatusAdapter = new SetCustomerCreditStatusAdapter(staleReadPort);

		// PATCH commits first against the real adapter, bumping the row's version.
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

		// PATCH's committed rename must survive untouched.
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
