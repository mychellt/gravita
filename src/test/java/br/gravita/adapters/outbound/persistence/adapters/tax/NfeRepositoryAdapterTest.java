package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfeJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfeTransportInfo;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransportModality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfeRepositoryAdapterTest {

	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");

	@Mock
	private NfeJpaRepository repository;

	@Mock
	private NfePersistenceMapper mapper;

	@InjectMocks
	private NfeRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new NF-e marking its entity as new")
	void shouldSaveNewNfe() {
		final NfeDocument document = buildDocument(CompanyId.of(UUID.randomUUID()));
		final NfeJpaEntity entity = buildEntity(document.getId());
		final NfeJpaEntity saved = buildEntity(document.getId());
		when(mapper.map(document)).thenReturn(entity);
		when(repository.existsById(document.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(document);

		final NfeDocument result = adapter.save(document);

		assertThat(result).isSameAs(document);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing NF-e marking its entity as not new")
	void shouldSaveExistingNfeAsNotNew() {
		final NfeDocument document = buildDocument(CompanyId.of(UUID.randomUUID()));
		final NfeJpaEntity entity = buildEntity(document.getId());
		when(mapper.map(document)).thenReturn(entity);
		when(repository.existsById(document.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(document);

		adapter.save(document);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds an NF-e by id")
	void shouldFindNfeById() {
		final NfeDocument document = buildDocument(CompanyId.of(UUID.randomUUID()));
		final NfeJpaEntity entity = buildEntity(document.getId());
		when(repository.findById(document.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final Optional<NfeDocument> result = adapter.findById(document.getId());

		assertThat(result).contains(document);
		verify(repository).findById(document.getId().value());
	}

	@Test
	@DisplayName("Finds the NF-e authorized within the window")
	void shouldFindAuthorizedBetween() {
		final NfeDocument document = buildDocument(CompanyId.of(UUID.randomUUID()));
		final NfeJpaEntity entity = buildEntity(document.getId());
		when(repository.findByStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
				NfeDocumentStatus.AUTHORIZED, FROM, TO)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final List<NfeDocument> result = adapter.findAuthorizedBetween(FROM, TO);

		assertThat(result).containsExactly(document);
	}

	@Test
	@DisplayName("Finds the company's authorized NF-e within the window")
	void shouldFindAuthorizedByCompanyBetween() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final NfeDocument document = buildDocument(companyId);
		final NfeJpaEntity entity = buildEntity(document.getId());
		when(repository.findByIssuerCompanyIdAndStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
				companyId.value(), NfeDocumentStatus.AUTHORIZED, FROM, TO)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final List<NfeDocument> result = adapter.findAuthorizedByCompanyBetween(companyId, FROM, TO);

		assertThat(result).containsExactly(document);
	}

	@Test
	@DisplayName("Finds the company's authorized or cancelled NF-e within the window")
	void shouldFindAuthorizedOrCancelledByCompanyBetween() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final NfeDocument document = buildDocument(companyId);
		final NfeJpaEntity entity = buildEntity(document.getId());
		when(repository.findByIssuerCompanyIdAndStatusInAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
				companyId.value(), List.of(NfeDocumentStatus.AUTHORIZED, NfeDocumentStatus.CANCELLED), FROM, TO))
				.thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final List<NfeDocument> result = adapter.findAuthorizedOrCancelledByCompanyBetween(companyId, FROM, TO);

		assertThat(result).containsExactly(document);
	}

	@Test
	@DisplayName("Lists every NF-e of the company newest first, ordering by authorization and falling back to creation")
	void shouldFindAllByCompanyNewestFirst() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final NfeDocument oldDraft = buildDocument(companyId, Instant.parse("2028-01-01T00:00:00Z"));
		final NfeDocument recentDraft = buildDocument(companyId, Instant.parse("2028-02-10T00:00:00Z"));
		final NfeDocument authorized = authorizedAt(buildDocument(companyId, Instant.parse("2028-01-02T00:00:00Z")),
				Instant.parse("2028-02-20T00:00:00Z"));
		final List<NfeJpaEntity> entities = List.of(buildEntity(oldDraft.getId()), buildEntity(authorized.getId()),
				buildEntity(recentDraft.getId()));
		when(repository.findByIssuerCompanyId(companyId.value())).thenReturn(entities);
		when(mapper.map(same(entities.get(0)))).thenReturn(oldDraft);
		when(mapper.map(same(entities.get(1)))).thenReturn(authorized);
		when(mapper.map(same(entities.get(2)))).thenReturn(recentDraft);

		assertThat(adapter.findAllByCompany(companyId)).containsExactly(authorized, recentDraft, oldDraft);
	}

	@Test
	@DisplayName("Lists nothing for a company without NF-e")
	void shouldFindNothingForCompanyWithoutNfe() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		when(repository.findByIssuerCompanyId(companyId.value())).thenReturn(List.of());

		assertThat(adapter.findAllByCompany(companyId)).isEmpty();
	}

	private NfeDocument authorizedAt(final NfeDocument draft, final Instant authorizedAt) {
		return NfeDocument.builder().id(draft.getId()).issuerCompanyId(draft.getIssuerCompanyId())
				.naturezaOperacao(draft.getNaturezaOperacao()).cfop(draft.getCfop()).recipient(draft.getRecipient())
				.items(draft.getItems()).freight(draft.getFreight()).insurance(draft.getInsurance())
				.otherExpenses(draft.getOtherExpenses()).taxTotals(draft.getTaxTotals())
				.status(NfeDocumentStatus.AUTHORIZED).createdAt(draft.getCreatedAt()).documentSeries("1")
				.documentNumber(1L).authorizedAt(authorizedAt).build();
	}

	private NfeJpaEntity buildEntity(final NfeDocumentId id) {
		return NfeJpaEntity.builder().id(id.value()).build();
	}

	private NfeDocument buildDocument(final CompanyId companyId) {
		return buildDocument(companyId, Instant.now());
	}

	private NfeDocument buildDocument(final CompanyId companyId, final Instant createdAt) {
		final UUID productId = UUID.randomUUID();
		final TaxLineBreakdown icmsLine = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"),
				new BigDecimal("18"), new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(icmsLine));
		final NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		final NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81",
				PersonType.COMPANY, "Cliente PJ Teste", "123456789", "RJ");
		final NfeTransportInfo transport = new NfeTransportInfo(TransportModality.CIF, "Transportadora Teste", 1,
				new BigDecimal("10.500"), new BigDecimal("10.000"), "12345678901");
		return NfeDocument.draft()
				.id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(companyId)
				.originSalesOrderId(null)
				.naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop("5102"))
				.recipient(recipient)
				.items(List.of(item))
				.freight(new BigDecimal("15.00"))
				.insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO)
				.transport(transport)
				.referencedAccessKey(null)
				.additionalInfo("Informação adicional de teste")
				.paymentMethodType(PaymentMethodType.PIX)
				.taxTotals(TaxCalculationTotals.from(List.of(breakdown)))
				.createdAt(createdAt)
				.build();
	}
}
