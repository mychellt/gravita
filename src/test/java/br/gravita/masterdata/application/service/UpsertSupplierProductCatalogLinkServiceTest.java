package br.gravita.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.usercases.UpsertSupplierProductCatalogLinkService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpsertSupplierProductCatalogLinkServiceTest {

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	private UpsertSupplierProductCatalogLinkService service;

	private final UUID companyId = UUID.randomUUID();
	private final UUID supplierId = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();

	@Test
	@DisplayName("Creates a new link when none exists yet for the (company, supplier, code) key")
	void createsNewLinkWhenNoneExists() {
		service = new UpsertSupplierProductCatalogLinkService(linkRepositoryPort);
		when(linkRepositoryPort.findByCompanySupplierAndCode(CompanyId.of(companyId), SupplierId.of(supplierId), "SKU-001"))
				.thenReturn(Optional.empty());
		when(linkRepositoryPort.save(any(SupplierProductCatalogLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final Instant purchasedAt = Instant.parse("2026-01-10T12:00:00Z");
		final SupplierProductCatalogLink result = service.execute(new UpsertSupplierProductCatalogLinkCommand(companyId,
				supplierId, "SKU-001", productId, new BigDecimal("10.00"), purchasedAt));

		assertThat(result.getProductId()).isEqualTo(productId);
		assertThat(result.getLastPurchaseUnitValue()).isEqualByComparingTo("10.00");
		assertThat(result.getLastPurchaseAt()).isEqualTo(purchasedAt);
	}

	@Test
	@DisplayName("BR7: a repeat purchase only refreshes price/date on the existing link, keeping its productId and id")
	void refreshesPriceAndDateOnExistingLink() {
		service = new UpsertSupplierProductCatalogLinkService(linkRepositoryPort);
		final Instant firstPurchase = Instant.parse("2026-01-10T12:00:00Z");
		final SupplierProductCatalogLink existing = SupplierProductCatalogLink.create(CompanyId.of(companyId),
				SupplierId.of(supplierId), "SKU-001", productId, new BigDecimal("10.00"), firstPurchase);
		when(linkRepositoryPort.findByCompanySupplierAndCode(CompanyId.of(companyId), SupplierId.of(supplierId), "SKU-001"))
				.thenReturn(Optional.of(existing));
		final ArgumentCaptor<SupplierProductCatalogLink> captor = ArgumentCaptor.forClass(SupplierProductCatalogLink.class);
		when(linkRepositoryPort.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

		final Instant secondPurchase = firstPurchase.plusSeconds(86_400);
		service.execute(new UpsertSupplierProductCatalogLinkCommand(companyId, supplierId, "SKU-001", productId,
				new BigDecimal("12.50"), secondPurchase));

		assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
		assertThat(captor.getValue().getLastPurchaseUnitValue()).isEqualByComparingTo("12.50");
		assertThat(captor.getValue().getLastPurchaseAt()).isEqualTo(secondPurchase);
	}
}
