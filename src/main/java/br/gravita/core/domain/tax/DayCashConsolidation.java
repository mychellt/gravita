package br.gravita.core.domain.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record DayCashConsolidation(List<PosSessionId> sessionIds, BigDecimal totalOpeningAmount,
		Map<PaymentMethodType, BigDecimal> totalAmountsByPaymentMethod, BigDecimal totalSangriaAmount,
		BigDecimal totalSuprimentoAmount, BigDecimal totalExpectedCashAmount, int totalSaleCount) {

	public static DayCashConsolidation of(List<CashClosingReport> reports) {
		if (reports == null || reports.isEmpty()) {
			throw new BusinessRuleException("At least one CashClosingReport is required to consolidate");
		}

		List<PosSessionId> sessionIds = reports.stream().map(CashClosingReport::getSessionId).toList();

		BigDecimal totalOpeningAmount = reports.stream()
				.map(CashClosingReport::getOpeningAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Map<PaymentMethodType, BigDecimal> totalAmountsByPaymentMethod = new EnumMap<>(PaymentMethodType.class);
		for (CashClosingReport report : reports) {
			report.getExpectedAmountsByPaymentMethod()
					.forEach((method, amount) -> totalAmountsByPaymentMethod.merge(method, amount, BigDecimal::add));
		}

		BigDecimal totalSangriaAmount = reports.stream()
				.map(CashClosingReport::getTotalSangriaAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalSuprimentoAmount = reports.stream()
				.map(CashClosingReport::getTotalSuprimentoAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalExpectedCashAmount = reports.stream()
				.map(CashClosingReport::getExpectedCashAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int totalSaleCount = reports.stream().mapToInt(CashClosingReport::getSaleCount).sum();

		return new DayCashConsolidation(sessionIds, totalOpeningAmount, Map.copyOf(totalAmountsByPaymentMethod),
				totalSangriaAmount, totalSuprimentoAmount, totalExpectedCashAmount, totalSaleCount);
	}
}
