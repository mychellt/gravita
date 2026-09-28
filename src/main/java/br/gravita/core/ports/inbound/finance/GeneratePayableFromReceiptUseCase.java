package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;
import java.util.List;

/**
 * UC-M8-11: creates one {@code Payable} per installment of a confirmed purchase
 * receipt's payment terms, with {@code PURCHASE_RECEIPT} origin and {@code OPEN}
 * status. Invoked by M6's {@code ConfirmPurchaseReceiptUseCase}, never directly by
 * a user. Idempotent per {@code purchaseReceiptRef}: repeating a call returns the
 * payables already generated instead of creating duplicates.
 */
public interface GeneratePayableFromReceiptUseCase {

	List<Payable> execute(GeneratePayableFromReceiptCommand command);
}
