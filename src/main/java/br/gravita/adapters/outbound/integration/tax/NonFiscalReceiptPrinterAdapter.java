package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.ports.outbound.tax.PrintNonFiscalReceiptPort;
import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class NonFiscalReceiptPrinterAdapter implements PrintNonFiscalReceiptPort {

	private static final Logger log = LoggerFactory.getLogger(NonFiscalReceiptPrinterAdapter.class);

	@Override
	public void print(CashClosingReport report) {
		log.info("Z report for session {}:\n{}", report.getSessionId().value(), render(report));
	}

	private String render(CashClosingReport report) {
		StringBuilder text = new StringBuilder();
		text.append("=== CASH CLOSING REPORT (Z REPORT) ===\n");
		text.append("Register: ").append(report.getRegisterId()).append('\n');
		text.append("Operator: ").append(report.getOperatorId()).append('\n');
		text.append("Opened at: ").append(report.getOpenedAt()).append('\n');
		text.append("Closed at: ").append(report.getClosedAt()).append('\n');
		text.append("Opening amount: ").append(report.getOpeningAmount()).append('\n');
		text.append("Sale count: ").append(report.getSaleCount()).append('\n');
		text.append("--- Sales by payment method (expected / counted) ---\n");
		for (Map.Entry<PaymentMethodType, BigDecimal> entry : report.getExpectedAmountsByPaymentMethod().entrySet()) {
			BigDecimal counted = report.getCountedAmountsByPaymentMethod().get(entry.getKey());
			text.append(entry.getKey()).append(": ").append(entry.getValue())
					.append(" / ").append(counted == null ? "not counted" : counted).append('\n');
		}
		text.append("Sangria total: ").append(report.getTotalSangriaAmount()).append('\n');
		text.append("Suprimento total: ").append(report.getTotalSuprimentoAmount()).append('\n');
		text.append("Expected cash amount: ").append(report.getExpectedCashAmount()).append('\n');
		return text.toString();
	}
}
