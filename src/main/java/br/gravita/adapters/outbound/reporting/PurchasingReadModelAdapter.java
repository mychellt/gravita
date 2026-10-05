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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@PersistenceAdapter
public class PurchasingReadModelAdapter implements PurchasingReadModelPort {

    private final PurchaseOrderJpaRepository purchaseOrderJpaRepository;
    private final PurchaseReceiptJpaRepository purchaseReceiptJpaRepository;
    private final ZoneId zone = ZoneId.systemDefault();

    PurchasingReadModelAdapter(final PurchaseOrderJpaRepository purchaseOrderJpaRepository,
                               final PurchaseReceiptJpaRepository purchaseReceiptJpaRepository) {
        this.purchaseOrderJpaRepository = purchaseOrderJpaRepository;
        this.purchaseReceiptJpaRepository = purchaseReceiptJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchasedOrder> purchasedOrders(final LocalDate from, final LocalDate to, final UUID companyId) {
        final Date start = Date.from(from.atStartOfDay(zone).toInstant());
        final Date end = Date.from(to.plusDays(1).atStartOfDay(zone).toInstant());
        final List<PurchaseOrderJpaEntity> orders = purchaseOrderJpaRepository.findApprovedCreatedBetween(start, end,
                PurchaseOrderStatus.CANCELLED);
        if (orders.isEmpty()) {
            return List.of();
        }
        final Map<UUID, List<LocalDate>> receivedOn = new HashMap<>();
        for (final PurchaseReceiptJpaEntity receipt : purchaseReceiptJpaRepository.findByOrderIdInAndStatus(
                orders.stream().map(PurchaseOrderJpaEntity::getId).toList(), PurchaseReceiptStatus.CONFIRMED)) {
            receivedOn.computeIfAbsent(receipt.getOrderId(), id -> new ArrayList<>()).add(receipt.getModifiedAt().toLocalDate());
        }
        return orders.stream().map(order -> toPurchasedOrder(order, receivedOn.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    private PurchasedOrder toPurchasedOrder(final PurchaseOrderJpaEntity order, final List<LocalDate> receivedOn) {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;
        for (final PurchaseOrderItemEmbeddable item : order.getItems()) {
            quantity = quantity.add(item.getQuantity());
            value = value.add(item.getQuantity().multiply(item.getUnitPrice()));
        }
        return new PurchasedOrder(order.getSupplierId(), order.getCreatedAt().toLocalDate(), quantity, value, receivedOn);
    }
}
