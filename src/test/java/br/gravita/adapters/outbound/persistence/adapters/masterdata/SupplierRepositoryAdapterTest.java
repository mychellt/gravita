package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.SupplierPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.SupplierJpaRepository;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierRepositoryAdapterTest {

	private static final Address VALID_ADDRESS =
			new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000");

	@Mock
	private SupplierJpaRepository repository;

	@Mock
	private SupplierPersistenceMapper mapper;

	@InjectMocks
	private SupplierRepositoryAdapter adapter;

	@Test
	@DisplayName("Persists a new supplier using the application-assigned id")
	void shouldPersistNewSupplierWithApplicationAssignedId() {
		final Supplier supplier = buildSupplier();
		final SupplierJpaEntity entity = buildEntity(supplier.getId());
		final SupplierJpaEntity saved = buildEntity(supplier.getId());
		when(mapper.map(supplier)).thenReturn(entity);
		when(repository.existsById(supplier.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(supplier);

		final Supplier result = adapter.save(supplier);

		assertThat(result).isSameAs(supplier);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Updates an existing supplier marking its entity as not new")
	void shouldUpdateExistingSupplierAsNotNew() {
		final Supplier supplier = buildSupplier();
		final SupplierJpaEntity entity = buildEntity(supplier.getId());
		when(mapper.map(supplier)).thenReturn(entity);
		when(repository.existsById(supplier.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(supplier);

		adapter.save(supplier);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a supplier by id")
	void shouldFindSupplierById() {
		final Supplier supplier = buildSupplier();
		final SupplierJpaEntity entity = buildEntity(supplier.getId());
		when(repository.findById(supplier.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(supplier);

		final Optional<Supplier> result = adapter.findById(supplier.getId());

		assertThat(result).contains(supplier);
		verify(repository).findById(supplier.getId().value());
	}

	@Test
	@DisplayName("Returns empty when the supplier does not exist")
	void shouldReturnEmptyWhenSupplierDoesNotExist() {
		final SupplierId id = SupplierId.of(UUID.randomUUID());
		when(repository.findById(id.value())).thenReturn(Optional.empty());

		assertThat(adapter.findById(id)).isEmpty();
	}

	private SupplierJpaEntity buildEntity(final SupplierId id) {
		return SupplierJpaEntity.builder().id(id.value()).build();
	}

	private Supplier buildSupplier() {
		return Supplier.of(SupplierId.of(UUID.randomUUID()), Document.cnpj("11222333000181"), "Acme Supplies",
				List.of(VALID_ADDRESS), List.of(), null, null, null, null);
	}
}
