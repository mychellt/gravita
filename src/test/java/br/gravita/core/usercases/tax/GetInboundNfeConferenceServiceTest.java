package br.gravita.core.usercases.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.tax.GetInboundNfeConferenceQuery;
import br.gravita.core.ports.inbound.tax.InboundNfeConference;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
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
class GetInboundNfeConferenceServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private GetInboundNfeConferenceService service;

	private final UserId callerId = UserId.generate();
	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		final OwnedInboundNfeLoader loader = new OwnedInboundNfeLoader(inboundNfeRepositoryPort,
				new CallerCompanyResolver(userRepositoryPort));
		service = new GetInboundNfeConferenceService(loader,
				new InboundNfeResolver(supplierRepositoryPort, linkRepositoryPort), productRepositoryPort);
	}

	@Test
	@DisplayName("AC1: marks the supplier resolved when its CNPJ matches a registered Supplier")
	void marksTheSupplierResolvedWhenTheCnpjMatches() {
		final InboundNfe nfe = inboundNfe(companyId);
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection())).thenReturn(List.of());

		final InboundNfeConference conference = service.execute(new GetInboundNfeConferenceQuery(nfe.getId().value(), callerId));

		assertThat(conference.supplierResolved()).isTrue();
		assertThat(conference.supplierId()).isEqualTo(supplierId.value());
	}

	@Test
	@DisplayName("AC1: leaves the supplier unresolved, without failing, when no Supplier matches the CNPJ")
	void leavesTheSupplierUnresolvedWhenNothingMatches() {
		final InboundNfe nfe = inboundNfe(companyId);
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.empty());

		final InboundNfeConference conference = service.execute(new GetInboundNfeConferenceQuery(nfe.getId().value(), callerId));

		assertThat(conference.supplierResolved()).isFalse();
		assertThat(conference.supplierId()).isNull();
		assertThat(conference.items()).hasSize(1);
		assertThat(conference.items().get(0).resolved()).isFalse();
		verify(linkRepositoryPort, never()).findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection());
	}

	@Test
	@DisplayName("AC4: pre-fills the product of an item whose supplier code has a link")
	void preFillsTheProductOfALinkedItem() {
		final InboundNfe nfe = inboundNfe(companyId);
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection()))
				.thenReturn(List.of(link("1.5000")));
		when(productRepositoryPort.findAllByIds(anyCollection()))
				.thenReturn(List.of(ProductDomain.builder().id(productId).internalCode("PARAF-M8").build()));

		final InboundNfeConference.Item item = service
				.execute(new GetInboundNfeConferenceQuery(nfe.getId().value(), callerId)).items().get(0);

		assertThat(item.resolved()).isTrue();
		assertThat(item.productId()).isEqualTo(productId);
		assertThat(item.productLabel()).isEqualTo("PARAF-M8");
		assertThat(item.description()).isEqualTo("Parafuso Sextavado M8");
		assertThat(item.priceDelta()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("AC8: reports the last price and the delta when the NF-e's unit value differs")
	void reportsThePriceVariance() {
		final InboundNfe nfe = inboundNfe(companyId);
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection()))
				.thenReturn(List.of(link("1.2000")));
		when(productRepositoryPort.findAllByIds(anyCollection())).thenReturn(List.of());

		final InboundNfeConference.Item item = service
				.execute(new GetInboundNfeConferenceQuery(nfe.getId().value(), callerId)).items().get(0);

		assertThat(item.unitValue()).isEqualByComparingTo("1.5000");
		assertThat(item.lastPurchaseUnitValue()).isEqualByComparingTo("1.2000");
		assertThat(item.priceDelta()).isEqualByComparingTo("0.3000");
		assertThat(item.lastPurchaseAt()).isEqualTo(Instant.parse("2025-12-01T12:00:00Z"));
	}

	@Test
	@DisplayName("AC9: an NF-e of another company looks nonexistent to the caller")
	void hidesTheInboundNfeOfAnotherCompany() {
		final InboundNfe nfe = inboundNfe(CompanyId.of(UUID.randomUUID()));
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));

		assertThatThrownBy(() -> service.execute(new GetInboundNfeConferenceQuery(nfe.getId().value(), callerId)))
				.isInstanceOf(InboundNfeNotFoundException.class);
	}

	private void callerBelongsTo(final CompanyId callerCompanyId) {
		when(userRepositoryPort.findById(callerId))
				.thenReturn(Optional.of(User.builder().companyId(callerCompanyId.value()).build()));
	}

	private SupplierProductCatalogLink link(final String lastPurchaseUnitValue) {
		return SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", productId,
				new BigDecimal(lastPurchaseUnitValue), Instant.parse("2025-12-01T12:00:00Z"));
	}

	private Supplier supplier() {
		return Supplier.of(supplierId, Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA",
				List.of(new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000")), List.of(), null,
				null, null, null);
	}

	private InboundNfe inboundNfe(final CompanyId nfeCompanyId) {
		return InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(nfeCompanyId)
				.accessKey("35240111222333000181550010000012345123456789")
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor Exemplo LTDA")
				.issuedAt(Instant.parse("2026-01-10T12:00:00Z"))
				.items(List.of(new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
						new BigDecimal("10"), new BigDecimal("1.5000"), new BigDecimal("150.00"), BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)))
				.totals(new InboundNfeTotals(new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						new BigDecimal("150.00")))
				.xmlStorageRef("xml-ref-1")
				.build();
	}
}
