package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConfirmInboundNfeReceiptRequest(List<ConferenceItem> conferenceResult) {

	public record ConferenceItem(UUID itemRef, BigDecimal orderedQty, BigDecimal receivedQty) {
	}

	public ConfirmInboundNfeReceiptCommand toCommand(UUID inboundNfeId) {
		List<ConfirmInboundNfeReceiptCommand.ConferenceItem> items = conferenceResult == null ? List.of()
				: conferenceResult.stream()
						.map(item -> new ConfirmInboundNfeReceiptCommand.ConferenceItem(item.itemRef(),
								item.orderedQty(), item.receivedQty()))
						.toList();
		return new ConfirmInboundNfeReceiptCommand(inboundNfeId, items);
	}
}
