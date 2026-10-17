package br.gravita.core.usercases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierNotFoundException;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpsertSupplierProductCatalogLinkServiceTest {

	private static final Instant PURCHASED_AT = Instant.parse("2026-01-10T12:00:00Z");

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private UpsertSupplierProductCatalogLinkService service;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new UpsertSupplierProductCatalogLinkService(linkRepositoryPort, supplierRepositoryPort, productRepositoryPort);
	}

	@Test
	@DisplayName("Creates the link seeded with the given price and date when none exists")
	void createsTheLinkWhenNoneExists() {
		supplierAndProductExist();
		when(linkRepositoryPort.findBySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.empty());
		when(linkRepositoryPort.save(any(SupplierProductCatalogLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final SupplierProductCatalogLink link = service.execute(command(productId, "10.0000"));

		assertThat(link.getCompanyId()).isEqualTo(companyId);
		assertThat(link.getSupplierId()).isEqualTo(supplierId);
		assertThat(link.getSupplierProductCode()).isEqualTo("SKU-001");
		assertThat(link.getProductId()).isEqualTo(productId);
		assertThat(link.getLastPurchaseUnitValue()).isEqualByComparingTo("10.0000");
		assertThat(link.getLastPurchaseAt()).isEqualTo(PURCHASED_AT);
	}

	@Test
	@DisplayName("Refreshes the price and date of an existing link to the same product, keeping its id")
	void refreshesAnExistingLink() {
		supplierAndProductExist();
		final SupplierProductCatalogLink existing = SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001",
				productId, new BigDecimal("8.0000"), Instant.parse("2025-12-01T00:00:00Z"));
		when(linkRepositoryPort.findBySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.of(existing));
		when(linkRepositoryPort.save(any(SupplierProductCatalogLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final SupplierProductCatalogLink link = service.execute(command(productId, "10.0000"));

		assertThat(link.getId()).isEqualTo(existing.getId());
		assertThat(link.getLastPurchaseUnitValue()).isEqualByComparingTo("10.0000");
		assertThat(link.getLastPurchaseAt()).isEqualTo(PURCHASED_AT);
	}

	@Test
	@DisplayName("AC7: a product can be linked from another supplier at the same time, each link with its own price")
	void productCanCarryLinksFromSeveralSuppliers() {
		final SupplierId otherSupplierId = SupplierId.of(UUID.randomUUID());
		supplierAndProductExist();
		when(supplierRepositoryPort.findById(otherSupplierId)).thenReturn(Optional.of(supplier(otherSupplierId)));
		when(linkRepositoryPort.findBySupplierAndCode(any(), any(), any())).thenReturn(Optional.empty());
		when(linkRepositoryPort.save(any(SupplierProductCatalogLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(productId, "10.0000"));
		service.execute(new UpsertSupplierProductCatalogLinkCommand(companyId, otherSupplierId, "OTHER-9", productId,
				new BigDecimal("12.0000"), PURCHASED_AT));

		final ArgumentCaptor<SupplierProductCatalogLink> captor = ArgumentCaptor.forClass(SupplierProductCatalogLink.class);
		verify(linkRepositoryPort, org.mockito.Mockito.times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(SupplierProductCatalogLink::getProductId).containsOnly(productId);
		assertThat(captor.getAllValues()).extracting(SupplierProductCatalogLink::getSupplierId)
				.containsExactly(supplierId, otherSupplierId);
		assertThat(captor.getAllValues()).extracting(SupplierProductCatalogLink::getLastPurchaseUnitValue)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("10.0000"), new BigDecimal("12.0000"));
	}

	@Test
	@DisplayName("AC9: looks the existing link up inside the command's company")
	void looksTheLinkUpWithinTheCommandsCompany() {
		supplierAndProductExist();
		when(linkRepositoryPort.findBySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.empty());
		when(linkRepositoryPort.save(any(SupplierProductCatalogLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(productId, "10.0000"));

		verify(linkRepositoryPort).findBySupplierAndCode(companyId, supplierId, "SKU-001");
	}

	@Test
	@DisplayName("Never re-points an existing link to a different product")
	void rejectsRepointingAnExistingLink() {
		supplierAndProductExist();
		final SupplierProductCatalogLink existing = SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001",
				UUID.randomUUID(), new BigDecimal("8.0000"), PURCHASED_AT);
		when(linkRepositoryPort.findBySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.execute(command(productId, "10.0000")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already linked to another product");

		verify(linkRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a link to a product that does not exist")
	void rejectsAnUnknownProduct() {
		when(supplierRepositoryPort.findById(supplierId)).thenReturn(Optional.of(supplier(supplierId)));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command(productId, "10.0000")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Product not found");
	}

	@Test
	@DisplayName("Rejects a link for a supplier that does not exist")
	void rejectsAnUnknownSupplier() {
		when(supplierRepositoryPort.findById(supplierId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command(productId, "10.0000")))
				.isInstanceOf(SupplierNotFoundException.class);
	}

	private void supplierAndProductExist() {
		when(supplierRepositoryPort.findById(supplierId)).thenReturn(Optional.of(supplier(supplierId)));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(ProductDomain.builder().id(productId).build()));
	}

	private UpsertSupplierProductCatalogLinkCommand command(final UUID linkedProductId, final String unitValue) {
		return new UpsertSupplierProductCatalogLinkCommand(companyId, supplierId, "SKU-001", linkedProductId,
				new BigDecimal(unitValue), PURCHASED_AT);
	}

	private Supplier supplier(final SupplierId id) {
		return Supplier.of(id, Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA",
				List.of(new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000")), List.of(), null,
				null, null, null);
	}
}
