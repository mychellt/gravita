package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.CashClosingReport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public record CashClosingReportResponse(
		UUID id,
		UUID sessionId,
		UUID registerId,
		UUID operatorId,
		BigDecimal openingAmount,
		Map<PaymentMethodType, BigDecimal> expectedAmountsByPaymentMethod,
		Map<PaymentMethodType, BigDecimal> countedAmountsByPaymentMethod,
		Map<PaymentMethodType, BigDecimal> differencesByPaymentMethod,
		BigDecimal totalSangriaAmount,
		BigDecimal totalSuprimentoAmount,
		BigDecimal expectedCashAmount,
		int saleCount,
		Instant openedAt,
		Instant closedAt) {

	public static CashClosingReportResponse from(CashClosingReport report) {
		Map<PaymentMethodType, BigDecimal> differences = new EnumMap<>(PaymentMethodType.class);
		report.getCountedAmountsByPaymentMethod().keySet()
				.forEach(method -> differences.put(method, report.differenceFor(method)));

		return new CashClosingReportResponse(
				report.getId().value(),
				report.getSessionId().value(),
				report.getRegisterId(),
				report.getOperatorId(),
				report.getOpeningAmount(),
				report.getExpectedAmountsByPaymentMethod(),
				report.getCountedAmountsByPaymentMethod(),
				differences,
				report.getTotalSangriaAmount(),
				report.getTotalSuprimentoAmount(),
				report.getExpectedCashAmount(),
				report.getSaleCount(),
				report.getOpenedAt(),
				report.getClosedAt());
	}
}
