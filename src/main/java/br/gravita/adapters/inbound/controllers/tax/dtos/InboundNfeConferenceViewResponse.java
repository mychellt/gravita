package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.InboundNfeConferenceView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InboundNfeConferenceViewResponse(
		UUID id,
		String series,
		String number,
		String supplierName,
		boolean supplierResolved,
		UUID supplierId,
		List<ItemView> items) {

	public record ItemView(
			String supplierProductCode,
			String description,
			String ncm,
			String unit,
			BigDecimal quantity,
			BigDecimal unitValue,
			UUID resolvedProductId,
			String resolvedProductLabel,
			BigDecimal lastPurchaseUnitValue) {

		static ItemView from(final InboundNfeConferenceView.InboundNfeConferenceItemView view) {
			return new ItemView(view.supplierProductCode(), view.description(), view.ncm(), view.unit(),
					view.quantity(), view.unitValue(), view.resolvedProductId(), view.resolvedProductLabel(),
					view.lastPurchaseUnitValue());
		}
	}

	public static InboundNfeConferenceViewResponse from(final InboundNfeConferenceView view) {
		return new InboundNfeConferenceViewResponse(view.id(), view.series(), view.number(), view.supplierName(),
				view.supplierResolved(), view.supplierId(), view.items().stream().map(ItemView::from).toList());
	}
}
