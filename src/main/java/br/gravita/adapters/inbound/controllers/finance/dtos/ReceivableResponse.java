package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.ports.inbound.finance.ReceivableListItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableResponse(UUID id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
		LocalDate dueDate, Integer installments, ReceivableStatus status, UUID originDocumentRef,
		Integer installmentNumber, String customerName, BigDecimal remainingBalance) {

	public static ReceivableResponse from(final Receivable receivable) {
		return new ReceivableResponse(receivable.getId().value(), receivable.getCustomerId(), receivable.getOrigin(),
				receivable.getAmount(), receivable.getDueDate(), receivable.getInstallments(),
				receivable.getStatus(), receivable.getOriginDocumentRef(), receivable.getInstallmentNumber(), null, null);
	}

	public static ReceivableResponse from(final ReceivableListItem item) {
		final Receivable receivable = item.receivable();
		return new ReceivableResponse(receivable.getId().value(), receivable.getCustomerId(), receivable.getOrigin(),
				receivable.getAmount(), receivable.getDueDate(), receivable.getInstallments(), receivable.getStatus(),
				receivable.getOriginDocumentRef(), receivable.getInstallmentNumber(), item.customerName(), item.remainingBalance());
	}
}
