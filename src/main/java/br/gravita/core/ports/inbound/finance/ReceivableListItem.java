package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Receivable;
import java.math.BigDecimal;

/** One row of the receivables list: the title, its customer's name and what is still owed on it. */
public record ReceivableListItem(Receivable receivable, String customerName, BigDecimal remainingBalance) {
}
