package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsNewProductCommand;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.tax.ResolveInboundNfeItemAsNewProductService;
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
class ResolveInboundNfeItemAsNewProductServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	@Mock
	private RegisterProductPort registerProductPort;

	@Mock
	private UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase;

	private ResolveInboundNfeItemAsNewProductService service;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final Instant issuedAt = Instant.parse("2026-01-10T12:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ResolveInboundNfeItemAsNewProductService(inboundNfeRepositoryPort, supplierRepositoryPort,
				linkRepositoryPort, registerProductPort, upsertSupplierProductCatalogLinkUseCase);
	}

	@Test
	@DisplayName("BR4: registers the product and links it to the item's supplier code")
	void registersProductAndLinksIt() {
		final InboundNfe nfe = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findByCompanySupplierAndCode(companyId, supplierId, "SKU-001")).thenReturn(Optional.empty());
		final UUID newProductId = UUID.randomUUID();
		final ProductDomain created = ProductDomain.builder().id(newProductId).build();
		when(registerProductPort.execute(any(Context.class))).thenReturn(created);

		final ProductDomain productToRegister = ProductDomain.builder().internalCode("Parafuso Sextavado M8").build();
		final ProductDomain result = service.execute(new ResolveInboundNfeItemAsNewProductCommand(nfe.getId().value(), 0,
				productToRegister));

		assertThat(result.getId()).isEqualTo(newProductId);
		final ArgumentCaptor<UpsertSupplierProductCatalogLinkCommand> captor =
				ArgumentCaptor.forClass(UpsertSupplierProductCatalogLinkCommand.class);
		verify(upsertSupplierProductCatalogLinkUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(companyId.value());
		assertThat(captor.getValue().supplierId()).isEqualTo(supplierId.value());
		assertThat(captor.getValue().supplierProductCode()).isEqualTo("SKU-001");
		assertThat(captor.getValue().productId()).isEqualTo(newProductId);
		assertThat(captor.getValue().unitValue()).isEqualByComparingTo("1.5000");
		assertThat(captor.getValue().purchasedAt()).isEqualTo(issuedAt);
	}

	@Test
	@DisplayName("Rejects resolving when the NFe's supplier is not registered")
	void rejectsWhenSupplierUnresolved() {
		final InboundNfe nfe = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ResolveInboundNfeItemAsNewProductCommand(nfe.getId().value(), 0,
				ProductDomain.builder().build()))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects resolving an item that is already linked")
	void rejectsWhenItemAlreadyLinked() {
		final InboundNfe nfe = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findByCompanySupplierAndCode(companyId, supplierId, "SKU-001"))
				.thenReturn(Optional.of(SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001",
						UUID.randomUUID(), BigDecimal.TEN, issuedAt)));

		assertThatThrownBy(() -> service.execute(new ResolveInboundNfeItemAsNewProductCommand(nfe.getId().value(), 0,
				ProductDomain.builder().build()))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects an unknown inbound NFe id")
	void rejectsUnknownInboundNfe() {
		final UUID unknownId = UUID.randomUUID();
		when(inboundNfeRepositoryPort.findById(InboundNfeId.of(unknownId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ResolveInboundNfeItemAsNewProductCommand(unknownId, 0,
				ProductDomain.builder().build()))).isInstanceOf(InboundNfeNotFoundException.class);
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
