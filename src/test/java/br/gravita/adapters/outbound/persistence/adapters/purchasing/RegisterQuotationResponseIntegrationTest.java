package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.adapters.outbound.persistence.mappers.purchasing.QuotationPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.domain.purchasing.QuotationNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseCommand;
import br.gravita.core.usercases.purchasing.RegisterQuotationResponseService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Drives RegisterQuotationResponseUseCase (GRA-58) against a real H2-backed
 * repository (@DataJpaTest), proving the response set round-trips through
 * persistence rather than just what the mocked
 * RegisterQuotationResponseServiceTest can prove. Seeds the Quotation
 * directly through the adapter since SendQuotationUseCase (GRA-57/M6-02)
 * isn't built yet.
 */
@DataJpaTest
@Import({QuotationRepositoryAdapter.class, QuotationPersistenceMapperImpl.class})
class RegisterQuotationResponseIntegrationTest {

	@Autowired
	private QuotationRepositoryAdapter quotationRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private RegisterQuotationResponseService service;

	private final UUID productA = UUID.randomUUID();
	private final UUID productB = UUID.randomUUID();
	private final SupplierId supplierOne = SupplierId.of(UUID.randomUUID());
	private final SupplierId supplierTwo = SupplierId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new RegisterQuotationResponseService(quotationRepositoryAdapter);
	}

	@Test
	void registeringAResponsePersistsItsItemPricesAndDeadline() {
		QuotationId quotationId = seedQuotation();
		LocalDate deadline = LocalDate.now().plusDays(10);

		service.execute(new RegisterQuotationResponseCommand(quotationId, supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
				deadline));
		flushAndClear();

		Quotation persisted = quotationRepositoryAdapter.findById(quotationId).orElseThrow();
		assertThat(persisted.getResponses()).hasSize(1);
		assertThat(persisted.getResponses().get(0).supplierId()).isEqualTo(supplierOne);
		assertThat(persisted.getResponses().get(0).deadline()).isEqualTo(deadline);
		assertThat(persisted.getResponses().get(0).itemPrices()).hasSize(2);
	}

	@Test
	void reSubmittingAResponseReplacesThePriorOneInsteadOfDuplicatingIt() {
		QuotationId quotationId = seedQuotation();
		service.execute(new RegisterQuotationResponseCommand(quotationId, supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now().plusDays(10)));
		flushAndClear();

		service.execute(new RegisterQuotationResponseCommand(quotationId, supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.ONE), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now().plusDays(3)));
		flushAndClear();

		Quotation persisted = quotationRepositoryAdapter.findById(quotationId).orElseThrow();
		assertThat(persisted.getResponses()).hasSize(1);
		QuotationItemPrice pricedProductA = persisted.getResponses().get(0).itemPrices().stream()
				.filter(price -> price.productId().equals(productA)).findFirst().orElseThrow();
		assertThat(pricedProductA.unitPrice()).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void responsesFromDifferentSuppliersAreBothPersistedForComparison() {
		QuotationId quotationId = seedQuotation();
		service.execute(new RegisterQuotationResponseCommand(quotationId, supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now().plusDays(10)));
		flushAndClear();

		service.execute(new RegisterQuotationResponseCommand(quotationId, supplierTwo,
				List.of(new QuotationItemPrice(productA, BigDecimal.valueOf(8)), new QuotationItemPrice(productB, BigDecimal.TWO)),
				LocalDate.now().plusDays(5)));
		flushAndClear();

		Quotation persisted = quotationRepositoryAdapter.findById(quotationId).orElseThrow();
		assertThat(persisted.getResponses()).hasSize(2);
		assertThat(persisted.getResponses().stream().map(r -> r.supplierId()))
				.containsExactlyInAnyOrder(supplierOne, supplierTwo);
	}

	@Test
	void aResponseMissingAnItemIsRejectedBeforeAnythingIsPersisted() {
		QuotationId quotationId = seedQuotation();

		assertThatThrownBy(() -> service.execute(new RegisterQuotationResponseCommand(quotationId, supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN)), LocalDate.now())))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void registeringAResponseForAMissingQuotationIsRejected() {
		assertThatThrownBy(() -> service.execute(new RegisterQuotationResponseCommand(
				QuotationId.of(UUID.randomUUID()), supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN)), LocalDate.now())))
				.isInstanceOf(QuotationNotFoundException.class);
	}

	private QuotationId seedQuotation() {
		QuotationId id = QuotationId.of(UUID.randomUUID());
		Quotation quotation = Quotation.send(id, PurchaseRequestId.of(UUID.randomUUID()),
				List.of(new QuotationItem(productA, BigDecimal.TEN), new QuotationItem(productB, BigDecimal.valueOf(5))),
				List.of(supplierOne, supplierTwo));
		quotationRepositoryAdapter.save(quotation);
		flushAndClear();
		return id;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
