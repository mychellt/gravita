package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.core.domain.tax.ProductTaxProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductTaxProfileRepositoryAdapterTest {

	@Mock
	private ProductJpaRepository repository;

	@InjectMocks
	private ProductTaxProfileRepositoryAdapter adapter;

	@Test
	@DisplayName("Resolves a product's NCM by its id")
	void resolvesTheProductsNcmByItsId() {
		final UUID productId = UUID.randomUUID();
		final ProductJpaEntity entity = ProductJpaEntity.builder().id(productId).ncm("85171231").build();
		when(repository.findById(productId)).thenReturn(Optional.of(entity));

		final Optional<ProductTaxProfile> result = adapter.findByProductRef(productId.toString());

		assertThat(result).contains(new ProductTaxProfile(productId.toString(), "85171231"));
		verify(repository).findById(productId);
	}

	@Test
	@DisplayName("Returns empty when the product has a blank NCM")
	void returnsEmptyWhenTheProductHasABlankNcm() {
		final UUID productId = UUID.randomUUID();
		final ProductJpaEntity entity = ProductJpaEntity.builder().id(productId).ncm(" ").build();
		when(repository.findById(productId)).thenReturn(Optional.of(entity));

		assertThat(adapter.findByProductRef(productId.toString())).isEmpty();
	}

	@Test
	@DisplayName("Returns empty for an unknown product")
	void returnsEmptyForAnUnknownProduct() {
		final UUID productId = UUID.randomUUID();
		when(repository.findById(productId)).thenReturn(Optional.empty());

		assertThat(adapter.findByProductRef(productId.toString())).isEmpty();
	}

	@Test
	@DisplayName("Returns empty for a product reference that is not a UUID")
	void returnsEmptyForANonUuidProductRef() {
		assertThat(adapter.findByProductRef("not-a-uuid")).isEmpty();
		verifyNoInteractions(repository);
	}
}
