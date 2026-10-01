package br.gravita.purchasing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuotationTest {

	private final UUID productA = UUID.randomUUID();
	private final UUID productB = UUID.randomUUID();
	private final SupplierId supplierOne = SupplierId.of(UUID.randomUUID());
	private final SupplierId supplierTwo = SupplierId.of(UUID.randomUUID());

	@Test
	@DisplayName("Registering a response from a supplier the quotation was sent to adds it")
	void registeringAResponseFromASentSupplierAddsIt() {
		Quotation quotation = sentQuotation();

		Quotation updated = quotation.registerResponse(supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now().plusDays(5));

		assertThat(updated.getResponses()).hasSize(1);
		assertThat(updated.getResponses().get(0).supplierId()).isEqualTo(supplierOne);
	}

	@Test
	@DisplayName("Rejects a response from a supplier the quotation was not sent to")
	void registeringAResponseFromASupplierNotSentTheQuotationIsRejected() {
		Quotation quotation = sentQuotation();
		SupplierId strangerSupplier = SupplierId.of(UUID.randomUUID());

		assertThatThrownBy(() -> quotation.registerResponse(strangerSupplier,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("was not sent this quotation");
	}

	@Test
	@DisplayName("Rejects a response that does not price every quotation item")
	void registeringAResponseMissingAnItemIsRejected() {
		Quotation quotation = sentQuotation();

		assertThatThrownBy(() -> quotation.registerResponse(supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN)), LocalDate.now()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("itemPrices must cover every item");
	}

	@Test
	@DisplayName("Re-submitting a response from the same supplier replaces the prior one")
	void reSubmittingAResponseFromTheSameSupplierReplacesThePriorOne() {
		Quotation quotation = sentQuotation().registerResponse(supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now().plusDays(5));

		Quotation resubmitted = quotation.registerResponse(supplierOne,
				List.of(new QuotationItemPrice(productA, BigDecimal.ONE), new QuotationItemPrice(productB, BigDecimal.ONE)),
				LocalDate.now().plusDays(1));

		assertThat(resubmitted.getResponses()).hasSize(1);
		assertThat(resubmitted.getResponses().get(0).itemPrices())
				.contains(new QuotationItemPrice(productA, BigDecimal.ONE));
	}

	@Test
	@DisplayName("Responses from different suppliers are both retained")
	void responsesFromDifferentSuppliersAreBothRetained() {
		Quotation quotation = sentQuotation()
				.registerResponse(supplierOne,
						List.of(new QuotationItemPrice(productA, BigDecimal.TEN), new QuotationItemPrice(productB, BigDecimal.ONE)),
						LocalDate.now().plusDays(5))
				.registerResponse(supplierTwo,
						List.of(new QuotationItemPrice(productA, BigDecimal.valueOf(8)), new QuotationItemPrice(productB, BigDecimal.TWO)),
						LocalDate.now().plusDays(3));

		assertThat(quotation.getResponses()).hasSize(2);
		assertThat(quotation.getResponses().stream().map(r -> r.supplierId()))
				.containsExactlyInAnyOrder(supplierOne, supplierTwo);
	}

	@Test
	@DisplayName("A quotation must be sent to at least one supplier")
	void aQuotationMustBeSentToAtLeastOneSupplier() {
		assertThatThrownBy(() -> Quotation.send(QuotationId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()),
				List.of(new QuotationItem(productA, BigDecimal.ONE)), List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one supplier");
	}

	private Quotation sentQuotation() {
		return Quotation.send(QuotationId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				List.of(new QuotationItem(productA, BigDecimal.TEN), new QuotationItem(productB, BigDecimal.valueOf(5))),
				List.of(supplierOne, supplierTwo));
	}
}
