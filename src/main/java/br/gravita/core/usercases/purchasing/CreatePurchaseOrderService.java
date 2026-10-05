package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@UseCase
public class CreatePurchaseOrderService implements CreatePurchaseOrderUseCase {

    private final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;
    private final QuotationRepositoryPort quotationRepositoryPort;
    private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
    private final ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

    @Override
    public PurchaseOrderId execute(final CreatePurchaseOrderCommand command) {
        final var request = purchaseRequestRepositoryPort.findById(command.requestId())
                .orElseThrow(() -> new PurchaseRequestNotFoundException(command.requestId().value()));
        final var convertedRequest = request.convert();

        final List<PurchaseOrderItem> items = resolveItems(command);

        final var id = PurchaseOrderId.of(UUID.randomUUID());
        final var order = PurchaseOrder.create(id, command.requestId(), command.quotationId(),
                command.supplierId(), items, resolveApprovalRequired(items));

        final var saved = purchaseOrderRepositoryPort.save(order);

        purchaseRequestRepositoryPort.save(convertedRequest);

        return saved.getId();
    }

    private List<PurchaseOrderItem> resolveItems(final CreatePurchaseOrderCommand command) {
        if (command.quotationId() == null) {
            return command.items();
        }

        final Quotation quotation = quotationRepositoryPort.findById(QuotationId.of(command.quotationId()))
                .orElseThrow(() -> new QuotationNotFoundException(command.quotationId()));
        if (!quotation.getRequestId().equals(command.requestId())) {
            throw new BusinessRuleException(
                    "Quotation " + command.quotationId() + " does not belong to request " + command.requestId().value());
        }
        final QuotationResponse response = findResponse(quotation, command.supplierId());

        final Map<UUID, BigDecimal> pricesByProduct = response.itemPrices().stream()
                .collect(Collectors.toMap(QuotationItemPrice::productId, QuotationItemPrice::unitPrice));
        return quotation.getItems().stream()
                .map(item -> new PurchaseOrderItem(item.productId(), item.quantity(),
                        pricesByProduct.get(item.productId())))
                .toList();
    }

    private QuotationResponse findResponse(final Quotation quotation, final SupplierId supplierId) {
        return quotation.getResponses().stream()
                .filter(response -> response.supplierId().equals(supplierId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Supplier " + supplierId.value() + " has not responded to quotation " + quotation.getId().value()));
    }

    private boolean resolveApprovalRequired(final List<PurchaseOrderItem> items) {
        final BigDecimal total = PurchaseOrder.totalValue(items);
        return approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)
                .map(alcada -> alcada.getThresholdValue() != null && total.compareTo(alcada.getThresholdValue()) >= 0)
                .orElse(false);
    }
}
