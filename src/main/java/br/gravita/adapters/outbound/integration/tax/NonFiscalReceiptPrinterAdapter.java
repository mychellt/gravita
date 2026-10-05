package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.ports.outbound.tax.PrintNonFiscalReceiptPort;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
class NonFiscalReceiptPrinterAdapter implements PrintNonFiscalReceiptPort {

	@Override
	public void print(final CashClosingReport report) {
		log.info("Z report for session {}:\n{}", report.getSessionId().value(), render(report));
	}

	private String render(final CashClosingReport report) {
		final StringBuilder text = new StringBuilder();
		text.append("=== CASH CLOSING REPORT (Z REPORT) ===\n");
		text.append("Register: ").append(report.getRegisterId()).append('\n');
		text.append("Operator: ").append(report.getOperatorId()).append('\n');
		text.append("Opened at: ").append(report.getOpenedAt()).append('\n');
		text.append("Closed at: ").append(report.getClosedAt()).append('\n');
		text.append("Opening amount: ").append(report.getOpeningAmount()).append('\n');
		text.append("Sale count: ").append(report.getSaleCount()).append('\n');
		text.append("--- Sales by payment method (expected / counted) ---\n");
		for (final Map.Entry<PaymentMethodType, BigDecimal> entry : report.getExpectedAmountsByPaymentMethod().entrySet()) {
			final BigDecimal counted = report.getCountedAmountsByPaymentMethod().get(entry.getKey());
			text.append(entry.getKey()).append(": ").append(entry.getValue())
					.append(" / ").append(counted == null ? "not counted" : counted).append('\n');
		}
		text.append("Sangria total: ").append(report.getTotalSangriaAmount()).append('\n');
		text.append("Suprimento total: ").append(report.getTotalSuprimentoAmount()).append('\n');
		text.append("Expected cash amount: ").append(report.getExpectedCashAmount()).append('\n');
		return text.toString();
	}
}
