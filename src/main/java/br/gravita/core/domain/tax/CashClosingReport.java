package br.gravita.core.domain.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class CashClosingReport {

	private final CashClosingReportId id;
	private final PosSessionId sessionId;
	private final UUID registerId;
	private final UUID operatorId;
	private final BigDecimal openingAmount;
	private final Map<PaymentMethodType, BigDecimal> expectedAmountsByPaymentMethod;
	private final Map<PaymentMethodType, BigDecimal> countedAmountsByPaymentMethod;
	private final BigDecimal totalSangriaAmount;
	private final BigDecimal totalSuprimentoAmount;
	private final BigDecimal expectedCashAmount;
	private final int saleCount;
	private final Instant openedAt;
	private final Instant closedAt;

	public CashClosingReport(CashClosingReportId id, PosSessionId sessionId, UUID registerId, UUID operatorId,
			BigDecimal openingAmount, Map<PaymentMethodType, BigDecimal> expectedAmountsByPaymentMethod,
			Map<PaymentMethodType, BigDecimal> countedAmountsByPaymentMethod, BigDecimal totalSangriaAmount,
			BigDecimal totalSuprimentoAmount, int saleCount, Instant openedAt, Instant closedAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.sessionId = Objects.requireNonNull(sessionId, "sessionId is required");
		this.registerId = Objects.requireNonNull(registerId, "registerId is required");
		this.operatorId = Objects.requireNonNull(operatorId, "operatorId is required");
		this.openingAmount = Objects.requireNonNull(openingAmount, "openingAmount is required");
		this.expectedAmountsByPaymentMethod = copyOf(expectedAmountsByPaymentMethod);
		this.countedAmountsByPaymentMethod = copyOf(countedAmountsByPaymentMethod);
		this.totalSangriaAmount = zeroIfNull(totalSangriaAmount);
		this.totalSuprimentoAmount = zeroIfNull(totalSuprimentoAmount);
		if (saleCount < 0) {
			throw new BusinessRuleException("saleCount cannot be negative: " + saleCount);
		}
		this.saleCount = saleCount;
		this.openedAt = Objects.requireNonNull(openedAt, "openedAt is required");
		this.closedAt = Objects.requireNonNull(closedAt, "closedAt is required");
		this.expectedCashAmount = this.openingAmount
				.add(this.expectedAmountsByPaymentMethod.getOrDefault(PaymentMethodType.CASH, BigDecimal.ZERO))
				.add(this.totalSuprimentoAmount)
				.subtract(this.totalSangriaAmount);
	}

	public static CashClosingReport close(CashClosingReportId id, PosSessionId sessionId, UUID registerId,
			UUID operatorId, BigDecimal openingAmount, Map<PaymentMethodType, BigDecimal> expectedAmountsByPaymentMethod,
			Map<PaymentMethodType, BigDecimal> countedAmountsByPaymentMethod, BigDecimal totalSangriaAmount,
			BigDecimal totalSuprimentoAmount, int saleCount, Instant openedAt, Instant closedAt) {
		return new CashClosingReport(id, sessionId, registerId, operatorId, openingAmount,
				expectedAmountsByPaymentMethod, countedAmountsByPaymentMethod, totalSangriaAmount, totalSuprimentoAmount,
				saleCount, openedAt, closedAt);
	}

	public static CashClosingReport of(CashClosingReportId id, PosSessionId sessionId, UUID registerId,
			UUID operatorId, BigDecimal openingAmount, Map<PaymentMethodType, BigDecimal> expectedAmountsByPaymentMethod,
			Map<PaymentMethodType, BigDecimal> countedAmountsByPaymentMethod, BigDecimal totalSangriaAmount,
			BigDecimal totalSuprimentoAmount, int saleCount, Instant openedAt, Instant closedAt) {
		return new CashClosingReport(id, sessionId, registerId, operatorId, openingAmount,
				expectedAmountsByPaymentMethod, countedAmountsByPaymentMethod, totalSangriaAmount, totalSuprimentoAmount,
				saleCount, openedAt, closedAt);
	}

	public BigDecimal differenceFor(PaymentMethodType method) {
		BigDecimal counted = countedAmountsByPaymentMethod.get(method);
		if (counted == null) {
			return null;
		}
		BigDecimal expected = expectedAmountsByPaymentMethod.getOrDefault(method, BigDecimal.ZERO);
		return counted.subtract(expected);
	}

	private static Map<PaymentMethodType, BigDecimal> copyOf(Map<PaymentMethodType, BigDecimal> source) {
		if (source == null || source.isEmpty()) {
			return Map.of();
		}
		return Map.copyOf(source);
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
