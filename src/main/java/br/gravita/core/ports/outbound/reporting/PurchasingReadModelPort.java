package br.gravita.core.ports.outbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read-only view of the purchase orders and receipts the reporting context needs. */
public interface PurchasingReadModelPort {

	/**
	 * The purchase orders placed over {@code [from, to]} (inclusive) that are committed, in no particular order:
	 * cancelled orders and those still waiting for approval are left out. Each carries the days on which its
	 * deliveries were confirmed, which may fall after {@code to}.
	 */
	List<PurchasedOrder> purchasedOrders(LocalDate from, LocalDate to, UUID companyId);

	/**
	 * {@code quantity} and {@code value} are summed over the order's items, the value at the ordered unit prices;
	 * {@code receivedOn} holds the day each confirmed receipt of the order was confirmed, and is empty while nothing
	 * has been received.
	 */
	record PurchasedOrder(UUID supplierId, LocalDate orderedOn, BigDecimal quantity, BigDecimal value,
			List<LocalDate> receivedOn) {
	}
}
