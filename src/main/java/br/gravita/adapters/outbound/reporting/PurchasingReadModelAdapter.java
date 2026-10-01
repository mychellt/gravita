package br.gravita.adapters.outbound.reporting;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseOrderJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseReceiptJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
import br.gravita.core.ports.outbound.reporting.PurchasingReadModelPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purchase orders carry no company, so {@code companyId} cannot narrow these reads yet and is accepted only to keep
 * the port stable for when they do. Orders and receipts record no business date of their own: an order's day is the
 * one it was created on and a receipt's the one it was confirmed on (a confirmed receipt is not changed again, so
 * its last modification is its confirmation), both in the server's time zone.
 */
@PersistenceAdapter
class PurchasingReadModelAdapter implements PurchasingReadModelPort {

	private final PurchaseOrderJpaRepository purchaseOrderJpaRepository;
	private final PurchaseReceiptJpaRepository purchaseReceiptJpaRepository;
	private final ZoneId zone = ZoneId.systemDefault();

	PurchasingReadModelAdapter(PurchaseOrderJpaRepository purchaseOrderJpaRepository,
			PurchaseReceiptJpaRepository purchaseReceiptJpaRepository) {
		this.purchaseOrderJpaRepository = purchaseOrderJpaRepository;
		this.purchaseReceiptJpaRepository = purchaseReceiptJpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PurchasedOrder> purchasedOrders(LocalDate from, LocalDate to, UUID companyId) {
		Date start = Date.from(from.atStartOfDay(zone).toInstant());
		Date end = Date.from(to.plusDays(1).atStartOfDay(zone).toInstant());
		List<PurchaseOrderJpaEntity> orders = purchaseOrderJpaRepository.findApprovedCreatedBetween(start, end,
				PurchaseOrderStatus.CANCELLED);
		if (orders.isEmpty()) {
			return List.of();
		}
		Map<UUID, List<LocalDate>> receivedOn = new HashMap<>();
		for (PurchaseReceiptJpaEntity receipt : purchaseReceiptJpaRepository.findByOrderIdInAndStatus(
				orders.stream().map(PurchaseOrderJpaEntity::getId).toList(), PurchaseReceiptStatus.CONFIRMED)) {
			receivedOn.computeIfAbsent(receipt.getOrderId(), id -> new ArrayList<>()).add(day(receipt.getModifiedAt()));
		}
		return orders.stream().map(order -> toPurchasedOrder(order, receivedOn.getOrDefault(order.getId(), List.of())))
				.toList();
	}

	private PurchasedOrder toPurchasedOrder(PurchaseOrderJpaEntity order, List<LocalDate> receivedOn) {
		BigDecimal quantity = BigDecimal.ZERO;
		BigDecimal value = BigDecimal.ZERO;
		for (PurchaseOrderItemEmbeddable item : order.getItems()) {
			quantity = quantity.add(item.getQuantity());
			value = value.add(item.getQuantity().multiply(item.getUnitPrice()));
		}
		return new PurchasedOrder(order.getSupplierId(), day(order.getCreatedAt()), quantity, value, receivedOn);
	}

	private LocalDate day(Date timestamp) {
		return timestamp.toInstant().atZone(zone).toLocalDate();
	}
}
