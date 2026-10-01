package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.ProductPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryAdapterTest {

	@Mock
	private ProductJpaRepository repository;

	@Mock
	private ProductPersistenceMapper mapper;

	@InjectMocks
	private ProductRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new product marking its entity as new")
	void shouldSaveNewProduct() {
		final ProductDomain product = buildProduct("SKU-1");
		final ProductJpaEntity entity = buildEntity(product.getId());
		final ProductJpaEntity saved = buildEntity(product.getId());
		when(mapper.map(product)).thenReturn(entity);
		when(repository.existsById(product.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(product);

		final ProductDomain result = adapter.save(product);

		assertThat(result).isSameAs(product);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing product marking its entity as not new")
	void shouldSaveExistingProductAsNotNew() {
		final ProductDomain product = buildProduct("SKU-1");
		final ProductJpaEntity entity = buildEntity(product.getId());
		when(mapper.map(product)).thenReturn(entity);
		when(repository.existsById(product.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(product);

		adapter.save(product);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a product by id")
	void shouldFindProductById() {
		final ProductDomain product = buildProduct("SKU-1");
		final ProductJpaEntity entity = buildEntity(product.getId());
		when(repository.findById(product.getId())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(product);

		final Optional<ProductDomain> result = adapter.get(product.getId());

		assertThat(result).contains(product);
		verify(repository).findById(product.getId());
	}

	@Test
	@DisplayName("Lists all saved products")
	void shouldListAllProducts() {
		final ProductDomain first = buildProduct("SKU-1");
		final ProductDomain second = buildProduct("SKU-2");
		final ProductJpaEntity firstEntity = buildEntity(first.getId());
		final ProductJpaEntity secondEntity = buildEntity(second.getId());
		when(repository.findAll()).thenReturn(List.of(firstEntity, secondEntity));
		when(mapper.map(same(firstEntity))).thenReturn(first);
		when(mapper.map(same(secondEntity))).thenReturn(second);

		final List<ProductDomain> result = adapter.findAll();

		assertThat(result).containsExactly(first, second);
	}

	@Test
	@DisplayName("Detects an already registered barcode")
	void shouldDetectExistingBarcode() {
		when(repository.existsByBarcodesContaining("7891234567895")).thenReturn(true);
		when(repository.existsByBarcodesContaining("0000000000000")).thenReturn(false);

		assertThat(adapter.existsByBarcode("7891234567895")).isTrue();
		assertThat(adapter.existsByBarcode("0000000000000")).isFalse();
		verify(repository).existsByBarcodesContaining("7891234567895");
	}

	private ProductJpaEntity buildEntity(final UUID id) {
		return ProductJpaEntity.builder().id(id).build();
	}

	private ProductDomain buildProduct(final String internalCode) {
		final ProductDomain product = ProductDomain.builder()
				.internalCode(internalCode)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.build();
		product.setId(UUID.randomUUID());
		return product;
	}
}
