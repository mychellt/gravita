package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.tax.ProductTaxProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ProductTaxProfileRepositoryAdapter.class)
class ProductTaxProfileRepositoryAdapterTest {

	@Autowired
	private ProductTaxProfileRepositoryAdapter repositoryAdapter;

	@Autowired
	private ProductJpaRepository jpaRepository;

	@Test
	@DisplayName("Resolves a product's NCM by its id")
	void resolvesTheProductsNcmByItsId() {
		UUID productId = UUID.randomUUID();
		ProductJpaEntity entity = ProductJpaEntity.builder()
				.id(productId)
				.internalCode("SKU-1")
				.type(ProductType.SIMPLE)
				.barcodes(List.of())
				.images(List.of())
				.status(ProductStatus.ACTIVE)
				.ncm("85171231")
				.build();
		entity.setNew(true);
		jpaRepository.save(entity);

		Optional<ProductTaxProfile> profile = repositoryAdapter.findByProductRef(productId.toString());

		assertThat(profile).contains(new ProductTaxProfile(productId.toString(), "85171231"));
	}

	@Test
	@DisplayName("Returns empty for an unknown product")
	void returnsEmptyForAnUnknownProduct() {
		assertThat(repositoryAdapter.findByProductRef(UUID.randomUUID().toString())).isEmpty();
	}

	@Test
	@DisplayName("Returns empty for a product reference that is not a UUID")
	void returnsEmptyForANonUuidProductRef() {
		assertThat(repositoryAdapter.findByProductRef("not-a-uuid")).isEmpty();
	}
}
