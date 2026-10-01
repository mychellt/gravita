package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettlementTest {

	private final ReceivableId receivableId = ReceivableId.of(UUID.randomUUID());
	private final Instant paidAt = Instant.parse("2026-09-25T00:00:00Z");

	private Settlement cnab(BigDecimal amount, BigDecimal interest, BigDecimal discount, Instant at) {
		return Settlement.automaticCnab(SettlementId.of(UUID.randomUUID()), receivableId, amount, interest, null,
				discount, null, at);
	}

	@Test
	@DisplayName("Gives an automatic CNAB settlement the CNAB method and defaults missing adjustments to zero")
	void anAutomaticCnabSettlementCarriesTheCnabMethodAndDefaultsMissingAdjustmentsToZero() {
		Settlement settlement = cnab(new BigDecimal("90.00"), new BigDecimal("2.50"), null, paidAt);

		assertThat(settlement.getMethod()).isEqualTo(SettlementMethod.AUTOMATIC_CNAB);
		assertThat(settlement.getInterest()).isEqualByComparingTo("2.50");
		assertThat(settlement.getFine()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(settlement.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(settlement.getSurcharge()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(settlement.getTimestamp()).isEqualTo(paidAt);
	}

	@Test
	@DisplayName("Computes the credited amount as the principal plus the discount granted")
	void theCreditedAmountIsThePrincipalPlusTheDiscountGranted() {
		assertThat(cnab(new BigDecimal("90.00"), null, new BigDecimal("10.00"), paidAt).creditedAmount())
				.isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("Rejects a non-positive amount and negative adjustments")
	void rejectsANonPositiveAmountAndNegativeAdjustments() {
		assertThatThrownBy(() -> cnab(BigDecimal.ZERO, null, null, paidAt)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> cnab(null, null, null, paidAt)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> cnab(BigDecimal.TEN, new BigDecimal("-1"), null, paidAt))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Recognises the same bank payment but not a different one")
	void recognisesTheSameBankPaymentButNotAnotherOne() {
		Settlement settlement = cnab(new BigDecimal("50.00"), null, null, paidAt);

		assertThat(settlement.isSamePaymentAs(cnab(new BigDecimal("50.0"), null, null, paidAt))).isTrue();
		assertThat(settlement.isSamePaymentAs(cnab(new BigDecimal("50.00"), null, null, paidAt.plusSeconds(86400))))
				.isFalse();
		assertThat(settlement.isSamePaymentAs(cnab(new BigDecimal("51.00"), null, null, paidAt))).isFalse();
	}

	@Test
	@DisplayName("Gives a manual settlement the manual method and the entered adjustments")
	void aManualSettlementCarriesTheManualMethodAndTheEnteredAdjustments() {
		Settlement settlement = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivableId,
				new BigDecimal("90.00"), new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"),
				new BigDecimal("4.00"), paidAt);

		assertThat(settlement.getMethod()).isEqualTo(SettlementMethod.MANUAL);
		assertThat(settlement.getInterest()).isEqualByComparingTo("1.00");
		assertThat(settlement.getFine()).isEqualByComparingTo("2.00");
		assertThat(settlement.getDiscount()).isEqualByComparingTo("3.00");
		assertThat(settlement.getSurcharge()).isEqualByComparingTo("4.00");
	}

	@Test
	@DisplayName("Computes the cash amount as principal plus interest, fine and surcharge, excluding the discount")
	void theCashAmountIsThePrincipalPlusInterestFineAndSurchargeButNotTheDiscount() {
		Settlement settlement = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivableId,
				new BigDecimal("90.00"), new BigDecimal("2.00"), new BigDecimal("1.00"), new BigDecimal("10.00"),
				new BigDecimal("0.50"), paidAt);

		assertThat(settlement.cashAmount()).isEqualByComparingTo("93.50");
	}

	@Test
	@DisplayName("Requires a settlement to apply to exactly one receivable or payable")
	void aSettlementAppliesToExactlyOneReceivableOrPayable() {
		PayableId payableId = PayableId.of(UUID.randomUUID());
		Settlement payment = Settlement.ofPayable(SettlementId.of(UUID.randomUUID()), payableId, BigDecimal.TEN, null,
				null, null, null, SettlementMethod.PIX, paidAt);

		assertThat(payment.isReceivableSide()).isFalse();
		assertThat(payment.getPayableId()).isEqualTo(payableId);
		assertThat(payment.getReceivableId()).isNull();
		assertThat(cnab(BigDecimal.TEN, null, null, paidAt).isReceivableSide()).isTrue();
		assertThatThrownBy(() -> Settlement.of(SettlementId.of(UUID.randomUUID()), null, BigDecimal.TEN, null, null,
				null, null, SettlementMethod.MANUAL, paidAt)).isInstanceOf(BusinessRuleException.class);
	}
}
