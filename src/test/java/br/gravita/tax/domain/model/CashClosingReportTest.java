package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.CashClosingReportId;
import br.gravita.core.domain.tax.DayCashConsolidation;
import br.gravita.core.domain.tax.PosSessionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CashClosingReportTest {

	private CashClosingReport close(final BigDecimal openingAmount, final Map<PaymentMethodType, BigDecimal> expected,
			final Map<PaymentMethodType, BigDecimal> counted, final BigDecimal sangria, final BigDecimal suprimento, final int saleCount) {
		return CashClosingReport.builder()
				.id(CashClosingReportId.of(UUID.randomUUID()))
				.sessionId(PosSessionId.of(UUID.randomUUID()))
				.registerId(UUID.randomUUID())
				.operatorId(UUID.randomUUID())
				.openingAmount(openingAmount)
				.expectedAmountsByPaymentMethod(expected)
				.countedAmountsByPaymentMethod(counted)
				.totalSangriaAmount(sangria)
				.totalSuprimentoAmount(suprimento)
				.saleCount(saleCount)
				.openedAt(Instant.now())
				.closedAt(Instant.now())
				.build();
	}

	@Test
	@DisplayName("Breaks the reconciliation totals down by payment method")
	void ac1ReconciliationTotalsAreBrokenDownByPaymentMethod() {
		final Map<PaymentMethodType, BigDecimal> expected = Map.of(
				PaymentMethodType.CASH, new BigDecimal("100.00"),
				PaymentMethodType.PIX, new BigDecimal("50.00"));

		final CashClosingReport report = close(new BigDecimal("100.00"), expected, Map.of(), BigDecimal.ZERO,
				BigDecimal.ZERO, 2);

		assertThat(report.getExpectedAmountsByPaymentMethod()).isEqualTo(expected);
	}

	@Test
	@DisplayName("Reports a null difference for a payment method that was not counted")
	void ac1DifferenceForAnUncountedMethodIsNull() {
		final CashClosingReport report = close(new BigDecimal("100.00"), Map.of(PaymentMethodType.CASH, new BigDecimal("50.00")),
				Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, 1);

		assertThat(report.differenceFor(PaymentMethodType.CASH)).isNull();
	}

	@Test
	@DisplayName("Computes the difference of a counted method as counted minus expected")
	void ac1DifferenceForACountedMethodIsCountedMinusExpected() {
		final CashClosingReport report = close(new BigDecimal("100.00"), Map.of(PaymentMethodType.CASH, new BigDecimal("50.00")),
				Map.of(PaymentMethodType.CASH, new BigDecimal("48.00")), BigDecimal.ZERO, BigDecimal.ZERO, 1);

		assertThat(report.differenceFor(PaymentMethodType.CASH)).isEqualByComparingTo("-2.00");
	}

	@Test
	@DisplayName("Includes the opening amount, cash movements and sale count in the report")
	void ac2TheReportIncludesOpeningAmountCashMovementsAndSaleCount() {
		final CashClosingReport report = close(new BigDecimal("100.00"), Map.of(PaymentMethodType.CASH, new BigDecimal("200.00")),
				Map.of(), new BigDecimal("30.00"), new BigDecimal("20.00"), 5);

		assertThat(report.getOpeningAmount()).isEqualByComparingTo("100.00");
		assertThat(report.getTotalSangriaAmount()).isEqualByComparingTo("30.00");
		assertThat(report.getTotalSuprimentoAmount()).isEqualByComparingTo("20.00");
		assertThat(report.getSaleCount()).isEqualTo(5);
		assertThat(report.getExpectedCashAmount()).isEqualByComparingTo("290.00");
	}

	@Test
	@DisplayName("Ignores non-cash payment methods in the expected cash amount")
	void ac2ExpectedCashAmountIgnoresNonCashPaymentMethods() {
		final CashClosingReport report = close(new BigDecimal("100.00"), Map.of(PaymentMethodType.PIX, new BigDecimal("500.00")),
				Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, 1);

		assertThat(report.getExpectedCashAmount()).isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("Consolidates multiple registers into a single day closing")
	void ac5MultipleRegistersCanBeConsolidatedIntoASingleDayClosing() {
		final CashClosingReport registerOne = close(new BigDecimal("100.00"),
				Map.of(PaymentMethodType.CASH, new BigDecimal("200.00"), PaymentMethodType.PIX, new BigDecimal("50.00")),
				Map.of(), new BigDecimal("10.00"), BigDecimal.ZERO, 3);
		final CashClosingReport registerTwo = close(new BigDecimal("50.00"),
				Map.of(PaymentMethodType.CASH, new BigDecimal("150.00")),
				Map.of(), BigDecimal.ZERO, new BigDecimal("20.00"), 2);

		final DayCashConsolidation consolidation = DayCashConsolidation.of(List.of(registerOne, registerTwo));

		assertThat(consolidation.totalOpeningAmount()).isEqualByComparingTo("150.00");
		assertThat(consolidation.totalAmountsByPaymentMethod().get(PaymentMethodType.CASH)).isEqualByComparingTo("350.00");
		assertThat(consolidation.totalAmountsByPaymentMethod().get(PaymentMethodType.PIX)).isEqualByComparingTo("50.00");
		assertThat(consolidation.totalSangriaAmount()).isEqualByComparingTo("10.00");
		assertThat(consolidation.totalSuprimentoAmount()).isEqualByComparingTo("20.00");
		assertThat(consolidation.totalSaleCount()).isEqualTo(5);
		assertThat(consolidation.totalExpectedCashAmount())
				.isEqualByComparingTo(registerOne.getExpectedCashAmount().add(registerTwo.getExpectedCashAmount()));
	}
}
