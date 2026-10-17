package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierProductCatalogLinkJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.SupplierProductCatalogLinkPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.SupplierProductCatalogLinkJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierProductCatalogLinkRepositoryAdapterTest {

	@Mock
	private SupplierProductCatalogLinkJpaRepository repository;

	private final SupplierProductCatalogLinkPersistenceMapper mapper =
			Mappers.getMapper(SupplierProductCatalogLinkPersistenceMapper.class);

	private SupplierProductCatalogLinkRepositoryAdapter adapter;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		adapter = new SupplierProductCatalogLinkRepositoryAdapter(repository, mapper);
	}

	@Test
	@DisplayName("Saves a new link and maps every field both ways")
	void savesANewLink() {
		final SupplierProductCatalogLink link = link();
		when(repository.existsById(link.getId().value())).thenReturn(false);
		when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(SupplierProductCatalogLinkJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		final SupplierProductCatalogLink saved = adapter.save(link);

		assertThat(saved.getId()).isEqualTo(link.getId());
		assertThat(saved.getCompanyId()).isEqualTo(companyId);
		assertThat(saved.getSupplierId()).isEqualTo(supplierId);
		assertThat(saved.getSupplierProductCode()).isEqualTo("SKU-001");
		assertThat(saved.getProductId()).isEqualTo(productId);
		assertThat(saved.getLastPurchaseUnitValue()).isEqualByComparingTo("10.0000");
		assertThat(saved.getLastPurchaseAt()).isEqualTo(link.getLastPurchaseAt());
	}

	@Test
	@DisplayName("AC9: looks a link up by company, supplier and code")
	void findsByCompanySupplierAndCode() {
		final SupplierProductCatalogLinkJpaEntity entity = mapper.map(link());
		when(repository.findByCompanyIdAndSupplierIdAndSupplierProductCode(companyId.value(), supplierId.value(), "SKU-001"))
				.thenReturn(Optional.of(entity));

		assertThat(adapter.findBySupplierAndCode(companyId, supplierId, "SKU-001")).isPresent();
		assertThat(adapter.findBySupplierAndCode(CompanyId.of(UUID.randomUUID()), supplierId, "SKU-001")).isEmpty();
	}

	@Test
	@DisplayName("Looks up many codes at once, and not at all for an empty list")
	void findsByManyCodes() {
		when(repository.findByCompanyIdAndSupplierIdAndSupplierProductCodeIn(companyId.value(), supplierId.value(),
				List.of("SKU-001"))).thenReturn(List.of(mapper.map(link())));

		assertThat(adapter.findBySupplierAndCodes(companyId, supplierId, List.of("SKU-001"))).hasSize(1);
		assertThat(adapter.findBySupplierAndCodes(companyId, supplierId, List.of())).isEmpty();
		verify(repository, never()).findByCompanyIdAndSupplierIdAndSupplierProductCodeIn(companyId.value(),
				supplierId.value(), List.of());
	}

	@Test
	@DisplayName("Lists the links of a product within the company")
	void findsAllByProduct() {
		when(repository.findByCompanyIdAndProductId(companyId.value(), productId)).thenReturn(List.of(mapper.map(link())));

		assertThat(adapter.findAllByProduct(companyId, productId)).hasSize(1);
	}

	private SupplierProductCatalogLink link() {
		return SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", productId, new BigDecimal("10.0000"),
				Instant.parse("2026-01-10T12:00:00Z"));
	}
}
