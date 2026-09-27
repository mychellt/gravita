package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ConfirmInboundNfeReceiptCommand(UUID inboundNfeId, List<ConferenceItem> conferenceResult) {

	public ConfirmInboundNfeReceiptCommand {
		Objects.requireNonNull(inboundNfeId, "inboundNfeId is required");
		conferenceResult = conferenceResult == null ? List.of() : List.copyOf(conferenceResult);
	}

	public record ConferenceItem(UUID itemRef, BigDecimal orderedQty, BigDecimal receivedQty) {
		public ConferenceItem {
			Objects.requireNonNull(itemRef, "itemRef is required");
			Objects.requireNonNull(orderedQty, "orderedQty is required");
			Objects.requireNonNull(receivedQty, "receivedQty is required");
		}
	}
}
