package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.CustomerPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.CustomerJpaRepository;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerRepositoryAdapterTest {

	@Mock
	private CustomerJpaRepository repository;

	@Mock
	private CustomerPersistenceMapper mapper;

	@InjectMocks
	private CustomerRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new customer marking its entity as new")
	void shouldSaveNewCustomer() {
		final CustomerDomain customer = buildCustomer("Maria Silva");
		final CustomerJpaEntity entity = buildEntity(customer.getId());
		final CustomerJpaEntity saved = buildEntity(customer.getId());
		when(mapper.map(customer)).thenReturn(entity);
		when(repository.existsById(customer.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(customer);

		final CustomerDomain result = adapter.save(customer);

		assertThat(result).isSameAs(customer);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing customer marking its entity as not new")
	void shouldSaveExistingCustomerAsNotNew() {
		final CustomerDomain customer = buildCustomer("Maria Silva");
		final CustomerJpaEntity entity = buildEntity(customer.getId());
		when(mapper.map(customer)).thenReturn(entity);
		when(repository.existsById(customer.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(customer);

		adapter.save(customer);

		assertThat(entity.isNew()).isFalse();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Propagates an optimistic locking failure raised on a stale update")
	void shouldPropagateOptimisticLockingFailureOnStaleUpdate() {
		final CustomerDomain customer = buildCustomer("Maria Silva");
		final CustomerJpaEntity entity = buildEntity(customer.getId());
		when(mapper.map(customer)).thenReturn(entity);
		when(repository.existsById(customer.getId())).thenReturn(true);
		when(repository.save(entity))
				.thenThrow(new ObjectOptimisticLockingFailureException(CustomerJpaEntity.class, customer.getId()));

		assertThatThrownBy(() -> adapter.save(customer))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	@DisplayName("Finds a customer by id")
	void shouldFindCustomerById() {
		final CustomerDomain customer = buildCustomer("Maria Silva");
		final CustomerJpaEntity entity = buildEntity(customer.getId());
		when(repository.findById(customer.getId())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(customer);

		final Optional<CustomerDomain> result = adapter.get(customer.getId());

		assertThat(result).contains(customer);
		verify(repository).findById(customer.getId());
	}

	@Test
	@DisplayName("Returns empty when the customer does not exist")
	void shouldReturnEmptyWhenCustomerDoesNotExist() {
		final UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThat(adapter.get(id)).isEmpty();
	}

	@Test
	@DisplayName("Lists all saved customers")
	void shouldListAllCustomers() {
		final CustomerDomain ana = buildCustomer("Ana");
		final CustomerDomain bruno = buildCustomer("Bruno");
		final CustomerJpaEntity anaEntity = buildEntity(ana.getId());
		final CustomerJpaEntity brunoEntity = buildEntity(bruno.getId());
		when(repository.findAll()).thenReturn(List.of(anaEntity, brunoEntity));
		when(mapper.map(same(anaEntity))).thenReturn(ana);
		when(mapper.map(same(brunoEntity))).thenReturn(bruno);

		final List<CustomerDomain> result = adapter.findAll();

		assertThat(result).containsExactly(ana, bruno);
	}

	@Test
	@DisplayName("Lists only the customers of the given company")
	void shouldListCustomersOfACompany() {
		final UUID companyId = UUID.randomUUID();
		final CustomerDomain ana = buildCustomer("Ana");
		final CustomerJpaEntity anaEntity = buildEntity(ana.getId());
		when(repository.findAllByCompanyId(companyId)).thenReturn(List.of(anaEntity));
		when(mapper.map(same(anaEntity))).thenReturn(ana);

		assertThat(adapter.findAllByCompanyId(companyId)).containsExactly(ana);
		verify(repository, never()).findAll();
	}

	@Test
	@DisplayName("Finds a customer by id only within the given company")
	void shouldFindCustomerByIdAndCompany() {
		final UUID companyId = UUID.randomUUID();
		final CustomerDomain customer = buildCustomer("Maria Silva");
		final CustomerJpaEntity entity = buildEntity(customer.getId());
		when(repository.findByIdAndCompanyId(customer.getId(), companyId)).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(customer);

		assertThat(adapter.findByIdAndCompanyId(customer.getId(), companyId)).contains(customer);
		verify(repository, never()).findById(any());
	}

	@Test
	@DisplayName("Returns empty when the customer exists but belongs to another company")
	void shouldReturnEmptyWhenCustomerBelongsToAnotherCompany() {
		final UUID id = UUID.randomUUID();
		final UUID companyId = UUID.randomUUID();
		when(repository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

		assertThat(adapter.findByIdAndCompanyId(id, companyId)).isEmpty();
	}

	private CustomerJpaEntity buildEntity(final UUID id) {
		return CustomerJpaEntity.builder().id(id).build();
	}

	private CustomerDomain buildCustomer(final String name) {
		final CustomerDomain customer = CustomerDomain.builder()
				.name(name)
				.documentDomain(Document.cpf("111.444.777-35"))
				.creditLimit(BigDecimal.ZERO)
				.currentBalance(BigDecimal.ZERO)
				.status(CustomerStatus.REGULAR)
				.build();
		customer.setId(UUID.randomUUID());
		return customer;
	}
}
