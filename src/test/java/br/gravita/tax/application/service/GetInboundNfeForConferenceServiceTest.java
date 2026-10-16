package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.tax.InboundNfeConferenceView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.tax.GetInboundNfeForConferenceService;
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
class GetInboundNfeForConferenceServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private GetInboundNfeForConferenceService service;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final Instant issuedAt = Instant.parse("2026-01-10T12:00:00Z");

	@BeforeEach
	void setUp() {
		service = new GetInboundNfeForConferenceService(inboundNfeRepositoryPort, supplierRepositoryPort, linkRepositoryPort,
				productRepositoryPort);
	}

	@Test
	@DisplayName("AC4: an item with an existing link comes back resolved, with its last purchase price")
	void itemWithExistingLinkComesBackResolved() {
		final InboundNfe nfe = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		final UUID linkedProductId = UUID.randomUUID();
		when(linkRepositoryPort.findByCompanySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.of(
				SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", linkedProductId, new BigDecimal("1.4000"),
						issuedAt.minusSeconds(86_400))));
		when(productRepositoryPort.get(linkedProductId))
				.thenReturn(Optional.of(ProductDomain.builder().id(linkedProductId).internalCode("PRF-M8").build()));

		final InboundNfeConferenceView view = service.execute(nfe.getId().value());

		assertThat(view.supplierResolved()).isTrue();
		assertThat(view.supplierId()).isEqualTo(supplierId.value());
		assertThat(view.items()).hasSize(1);
		final var itemView = view.items().get(0);
		assertThat(itemView.resolvedProductId()).isEqualTo(linkedProductId);
		assertThat(itemView.resolvedProductLabel()).isEqualTo("PRF-M8");
		assertThat(itemView.lastPurchaseUnitValue()).isEqualByComparingTo("1.4000");
	}

	@Test
	@DisplayName("AC2: an unresolved supplier leaves every item unresolved too, without a link lookup")
	void unresolvedSupplierLeavesItemsUnresolved() {
		final InboundNfe nfe = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.empty());

		final InboundNfeConferenceView view = service.execute(nfe.getId().value());

		assertThat(view.supplierResolved()).isFalse();
		assertThat(view.supplierId()).isNull();
		assertThat(view.items().get(0).resolvedProductId()).isNull();
		org.mockito.Mockito.verifyNoInteractions(linkRepositoryPort);
	}

	@Test
	@DisplayName("Rejects an unknown inbound NFe id")
	void rejectsUnknownInboundNfe() {
		final UUID unknownId = UUID.randomUUID();
		when(inboundNfeRepositoryPort.findById(InboundNfeId.of(unknownId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(unknownId)).isInstanceOf(InboundNfeNotFoundException.class);
	}

	private Supplier supplier() {
		final Address address = new Address("Rua Fornecedor", "100", null, "Centro", "Sao Paulo", "SP", "01000-000");
		return Supplier.of(supplierId, Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA", List.of(address),
				List.of(), null, null, 5, "1102");
	}

	private InboundNfe pendingInboundNfe() {
		return InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(companyId)
				.accessKey("35240111222333000181550010000012345123456789")
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor Exemplo LTDA")
				.issuedAt(issuedAt)
				.items(List.of(item()))
				.totals(totals())
				.xmlStorageRef("xml-ref-1")
				.build();
	}

	private InboundNfeItem item() {
		return new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN", new BigDecimal("10"),
				new BigDecimal("1.5000"), new BigDecimal("150.00"), new BigDecimal("27.00"), BigDecimal.ZERO,
				new BigDecimal("2.48"), new BigDecimal("11.40"));
	}

	private InboundNfeTotals totals() {
		return new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"),
				new BigDecimal("165.00"));
	}
}
