package br.gravita.core.usercases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindSupplierProductCatalogLinkServiceTest {

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	private FindSupplierProductCatalogLinkService service;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new FindSupplierProductCatalogLinkService(linkRepositoryPort);
	}

	@Test
	@DisplayName("Finds the link by supplier and supplier product code within the company")
	void findsBySupplierAndCode() {
		final SupplierProductCatalogLink link = link(supplierId, "SKU-001");
		when(linkRepositoryPort.findBySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.of(link));

		assertThat(service.bySupplierProductCode(companyId, supplierId, "SKU-001")).contains(link);
	}

	@Test
	@DisplayName("AC9: the same supplier and code in another company resolves to nothing")
	void doesNotResolveAcrossCompanies() {
		final CompanyId otherCompanyId = CompanyId.of(UUID.randomUUID());
		when(linkRepositoryPort.findBySupplierAndCode(otherCompanyId, supplierId, "SKU-001")).thenReturn(Optional.empty());

		assertThat(service.bySupplierProductCode(otherCompanyId, supplierId, "SKU-001")).isEmpty();
	}

	@Test
	@DisplayName("AC7: lists every supplier link of a product, each with its own last purchase")
	void listsTheLinksOfAProductAcrossSuppliers() {
		final List<SupplierProductCatalogLink> links = List.of(link(supplierId, "SKU-001"),
				link(SupplierId.of(UUID.randomUUID()), "OTHER-9"));
		when(linkRepositoryPort.findAllByProduct(companyId, productId)).thenReturn(links);

		assertThat(service.byProduct(companyId, productId)).isEqualTo(links);
	}

	private SupplierProductCatalogLink link(final SupplierId linkedSupplierId, final String code) {
		return SupplierProductCatalogLink.create(companyId, linkedSupplierId, code, productId, BigDecimal.TEN,
				Instant.parse("2026-01-10T12:00:00Z"));
	}
}
