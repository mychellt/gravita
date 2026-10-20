package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.ports.inbound.finance.PayableListItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PayableResponse(UUID id, UUID supplierId, PayableOrigin origin, BigDecimal amount, LocalDate dueDate,
		List<CostCenterShareResponse> costCenterSplit, PayableStatus status, UUID approvedBy,
		List<String> attachments, String supplierName) {

	public record CostCenterShareResponse(UUID costCenterId, BigDecimal percent) {
	}

	public static PayableResponse from(final Payable payable) {
		return new PayableResponse(payable.getId().value(), payable.getSupplierId(), payable.getOrigin(),
				payable.getAmount(), payable.getDueDate(),
				payable.getCostCenterSplit().stream()
						.map(share -> new CostCenterShareResponse(share.costCenterId(), share.percent())).toList(),
				payable.getStatus(), payable.getApprovedBy(), payable.getAttachments(), null);
	}

	public static PayableResponse from(final PayableListItem item) {
		final PayableResponse base = from(item.payable());
		return new PayableResponse(base.id(), base.supplierId(), base.origin(), base.amount(), base.dueDate(), base.costCenterSplit(),
				base.status(), base.approvedBy(), base.attachments(), item.supplierName());
	}
}
