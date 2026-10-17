package br.gravita.core.usercases.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemCommand;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolveInboundNfeItemServiceTest {

	private static final Instant ISSUED_AT = Instant.parse("2026-01-10T12:00:00Z");

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	@Mock
	private RegisterProductPort registerProductPort;

	@Mock
	private UpsertSupplierProductCatalogLinkUseCase upsertLinkUseCase;

	private ResolveInboundNfeItemService service;

	private final UserId callerId = UserId.generate();
	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ResolveInboundNfeItemService(
				new OwnedInboundNfeLoader(inboundNfeRepositoryPort, new CallerCompanyResolver(userRepositoryPort)),
				new InboundNfeResolver(supplierRepositoryPort, linkRepositoryPort), registerProductPort, upsertLinkUseCase);
	}

	@Test
	@DisplayName("AC6: links an existing product to the item without registering a new one")
	void linksAnExistingProductWithoutCreatingOne() {
		final InboundNfe nfe = unresolvedInboundNfe(companyId);

		service.execute(ResolveInboundNfeItemCommand.linkingExisting(callerId, nfe.getId().value(), 0, productId));

		verify(registerProductPort, never()).execute(any());
		final ArgumentCaptor<UpsertSupplierProductCatalogLinkCommand> captor =
				ArgumentCaptor.forClass(UpsertSupplierProductCatalogLinkCommand.class);
		verify(upsertLinkUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(companyId);
		assertThat(captor.getValue().supplierId()).isEqualTo(supplierId);
		assertThat(captor.getValue().supplierProductCode()).isEqualTo("SKU-001");
		assertThat(captor.getValue().productId()).isEqualTo(productId);
		assertThat(captor.getValue().unitValue()).isEqualByComparingTo("1.5000");
		assertThat(captor.getValue().purchasedAt()).isEqualTo(ISSUED_AT);
	}

	@Test
	@DisplayName("AC5: registers the operator-completed product as given and links it, seeded with the item's unit value")
	void registersTheProductAsGivenAndLinksIt() {
		final InboundNfe nfe = unresolvedInboundNfe(companyId);
		final ProductDomain completed = ProductDomain.builder().internalCode("Parafuso Sextavado M8").ncm("73181500")
				.purchaseUnit("UN").origin(0).basePrice(new BigDecimal("3.90")).build();
		final ProductDomain registered = ProductDomain.builder().id(productId).internalCode("Parafuso Sextavado M8").build();
		when(registerProductPort.execute(any(Context.class))).thenReturn(registered);

		service.execute(ResolveInboundNfeItemCommand.registeringNew(callerId, nfe.getId().value(), 0, completed));

		final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
		verify(registerProductPort).execute(contextCaptor.capture());
		assertThat(contextCaptor.getValue().getData(ProductDomain.class)).isSameAs(completed);
		assertThat(completed.getCstCsosnByState()).isNull();
		assertThat(completed.getDefaultCfopByOperation()).isNull();
		final ArgumentCaptor<UpsertSupplierProductCatalogLinkCommand> captor =
				ArgumentCaptor.forClass(UpsertSupplierProductCatalogLinkCommand.class);
		verify(upsertLinkUseCase).execute(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(productId);
		assertThat(captor.getValue().unitValue()).isEqualByComparingTo("1.5000");
	}

	@Test
	@DisplayName("Rejects resolving an item that already has a link")
	void rejectsAnAlreadyResolvedItem() {
		final InboundNfe nfe = inboundNfe(companyId);
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection())).thenReturn(List.of(
				SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", productId, BigDecimal.ONE, ISSUED_AT)));

		assertThatThrownBy(() -> service
				.execute(ResolveInboundNfeItemCommand.linkingExisting(callerId, nfe.getId().value(), 0, UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already linked");

		verify(upsertLinkUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Rejects resolving an item whose supplier is not registered, without registering the product")
	void rejectsAnUnresolvedSupplierBeforeRegisteringAnything() {
		final InboundNfe nfe = inboundNfe(companyId);
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(ResolveInboundNfeItemCommand.registeringNew(callerId, nfe.getId().value(),
				0, ProductDomain.builder().internalCode("X").build())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("No registered supplier");

		verify(registerProductPort, never()).execute(any());
		verify(upsertLinkUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Rejects an item position the NF-e does not have")
	void rejectsAnItemPositionOutOfRange() {
		final InboundNfe nfe = unresolvedInboundNfe(companyId);

		assertThatThrownBy(() -> service
				.execute(ResolveInboundNfeItemCommand.linkingExisting(callerId, nfe.getId().value(), 3, productId)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("no item at position 4");
	}

	@Test
	@DisplayName("AC9: cannot resolve the items of another company's NF-e")
	void cannotResolveTheItemsOfAnotherCompanysInboundNfe() {
		final InboundNfe nfe = inboundNfe(CompanyId.of(UUID.randomUUID()));
		callerBelongsTo(companyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));

		assertThatThrownBy(() -> service
				.execute(ResolveInboundNfeItemCommand.linkingExisting(callerId, nfe.getId().value(), 0, productId)))
				.isInstanceOf(InboundNfeNotFoundException.class);

		verify(upsertLinkUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Requires exactly one of existing product or new product")
	void requiresExactlyOneProductSource() {
		assertThatThrownBy(() -> new ResolveInboundNfeItemCommand(callerId, UUID.randomUUID(), 0, null, null))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> new ResolveInboundNfeItemCommand(callerId, UUID.randomUUID(), 0, productId,
				ProductDomain.builder().build())).isInstanceOf(BusinessRuleException.class);
	}

	private InboundNfe unresolvedInboundNfe(final CompanyId nfeCompanyId) {
		final InboundNfe nfe = inboundNfe(nfeCompanyId);
		callerBelongsTo(nfeCompanyId);
		when(inboundNfeRepositoryPort.findById(nfe.getId())).thenReturn(Optional.of(nfe));
		when(supplierRepositoryPort.findByDocument(nfe.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(nfeCompanyId), eq(supplierId), anyCollection())).thenReturn(List.of());
		return nfe;
	}

	private void callerBelongsTo(final CompanyId callerCompanyId) {
		when(userRepositoryPort.findById(callerId))
				.thenReturn(Optional.of(User.builder().companyId(callerCompanyId.value()).build()));
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
				.issuedAt(ISSUED_AT)
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
