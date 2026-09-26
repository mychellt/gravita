package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NfceSaleTest {

	private final PosSessionId sessionId = PosSessionId.of(UUID.randomUUID());

	private SaleItem oneUnitAt(String unitPrice) {
		return new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal(unitPrice), BigDecimal.ZERO);
	}

	@Test
	void ac1_anItemWithAPositiveQuantityIsAddedWithNoFurtherConfirmation() {
		NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(oneUnitAt("10.00")),
				null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, Instant.now());

		assertThat(sale.getItems()).hasSize(1);
		assertThat(sale.getStatus()).isEqualTo(NfceSaleStatus.DRAFT);
	}

	@Test
	void ac1_zeroOrNegativeQuantityIsRejected() {
		assertThatThrownBy(() -> new SaleItem(UUID.randomUUID(), BigDecimal.ZERO, new BigDecimal("10.00"), null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void ac2_anItemDiscountThatWouldMakeTheLineNegativeIsRejected() {
		assertThatThrownBy(() -> new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"),
				new BigDecimal("10.01"))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void ac3_multiplePaymentMethodsCanBeCombinedInTheSameSale() {
		NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(oneUnitAt("80.00")),
				null,
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("50.00")),
						new Payment(PaymentMethodType.CREDIT_CARD, new BigDecimal("30.00"))),
				null, Instant.now());

		assertThat(sale.getPayments()).hasSize(2);
	}

	@Test
	void ac4_changeGivenIsDerivedFromPaymentsMinusSaleTotalNeverEnteredDirectly() {
		NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(oneUnitAt("18.00")),
				null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("20.00"))), null, Instant.now());

		assertThat(sale.getSaleTotal()).isEqualByComparingTo("18.00");
		assertThat(sale.getChangeGiven()).isEqualByComparingTo("2.00");
	}

	@Test
	void ac5_paymentsThatDoNotCoverTheSaleTotalAreRejected() {
		assertThatThrownBy(() -> NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId,
				List.of(oneUnitAt("50.00")), null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("40.00"))),
				null, Instant.now())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void ac5_totalDiscountAboveTheSubtotalIsRejected() {
		assertThatThrownBy(() -> NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId,
				List.of(oneUnitAt("50.00")), new BigDecimal("50.01"),
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("50.00"))), null, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void ac6_customerCpfIsOptional() {
		NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(oneUnitAt("10.00")),
				null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, Instant.now());

		assertThat(sale.getCustomerCpf()).isNull();
	}

	@Test
	void ac6_aValidCpfIsNormalizedToDigitsOnly() {
		NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(oneUnitAt("10.00")),
				null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), "529.982.247-25",
				Instant.now());

		assertThat(sale.getCustomerCpf()).isEqualTo("52998224725");
	}

	@Test
	void ac6_anInvalidCpfIsRejected() {
		assertThatThrownBy(() -> NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId,
				List.of(oneUnitAt("10.00")), null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))),
				"111.111.111-11", Instant.now()))
				.isInstanceOf(br.gravita.core.domain.shared.BusinessRuleException.class);
	}

	@Test
	void aSaleMustHaveAtLeastOneItem() {
		assertThatThrownBy(() -> NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(), null,
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aSaleMustHaveAtLeastOnePayment() {
		assertThatThrownBy(() -> NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId,
				List.of(oneUnitAt("10.00")), null, List.of(), null, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	private NfceSale draftSale() {
		return NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, List.of(oneUnitAt("10.00")), null,
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, Instant.now());
	}

	@Test
	void ac1_authorizingADraftSaleMovesItToAuthorizedWithTheSefazProtocol() {
		NfceSale sale = draftSale();

		NfceSale authorized = sale.authorize("001", 42L, "35" + "0".repeat(42), "protocol-123");

		assertThat(authorized.getStatus()).isEqualTo(NfceSaleStatus.AUTHORIZED);
		assertThat(authorized.getDocumentSeries()).isEqualTo("001");
		assertThat(authorized.getDocumentNumber()).isEqualTo(42L);
		assertThat(authorized.getSefazProtocol()).isEqualTo("protocol-123");
		assertThat(authorized.isContingencyMode()).isFalse();
	}

	@Test
	void ac2_queuingADraftSaleForContingencyMovesItToPendingSyncWithNoProtocol() {
		NfceSale sale = draftSale();

		NfceSale queued = sale.queueForContingency("001", 43L, "35" + "0".repeat(42));

		assertThat(queued.getStatus()).isEqualTo(NfceSaleStatus.PENDING_SYNC);
		assertThat(queued.isContingencyMode()).isTrue();
		assertThat(queued.getSefazProtocol()).isNull();
	}

	@Test
	void anAlreadyAuthorizedSaleCannotBeIssuedAgain() {
		NfceSale authorized = draftSale().authorize("001", 1L, "35" + "0".repeat(42), "protocol-1");

		assertThatThrownBy(() -> authorized.authorize("001", 2L, "35" + "0".repeat(42), "protocol-2"))
				.isInstanceOf(BusinessRuleException.class);
	}
}
