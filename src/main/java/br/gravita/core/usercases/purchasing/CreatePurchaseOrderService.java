package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@UseCase
public class CreatePurchaseOrderService implements CreatePurchaseOrderUseCase {

	private final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;
	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	public CreatePurchaseOrderService(PurchaseRequestRepositoryPort purchaseRequestRepositoryPort,
			PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort) {
		this.purchaseRequestRepositoryPort = purchaseRequestRepositoryPort;
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.approvalAlcadaRepositoryPort = approvalAlcadaRepositoryPort;
	}

	@Override
	public PurchaseOrderId execute(CreatePurchaseOrderCommand command) {
		PurchaseRequest request = purchaseRequestRepositoryPort.findById(command.requestId())
				.orElseThrow(() -> new PurchaseRequestNotFoundException(command.requestId().value()));
		PurchaseRequest convertedRequest = request.convert();

		PurchaseOrderId id = PurchaseOrderId.of(UUID.randomUUID());
		PurchaseOrder order = PurchaseOrder.create(id, command.requestId(), command.quotationId(),
				command.supplierId(), command.items(), resolveApprovalRequired(command.items()));
		PurchaseOrder saved = purchaseOrderRepositoryPort.save(order);

		purchaseRequestRepositoryPort.save(convertedRequest);

		return saved.getId();
	}

	private boolean resolveApprovalRequired(List<PurchaseOrderItem> items) {
		BigDecimal total = PurchaseOrder.totalValue(items);
		return approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)
				.map(alcada -> alcada.getThresholdValue() != null && total.compareTo(alcada.getThresholdValue()) >= 0)
				.orElse(false);
	}
}
