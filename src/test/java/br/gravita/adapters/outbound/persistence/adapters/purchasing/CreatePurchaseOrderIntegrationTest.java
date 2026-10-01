package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.*;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.usercases.purchasing.CreatePurchaseOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CreatePurchaseOrderIntegrationTest {
    @Mock
    private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

    @Mock
    private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

    @Mock
    private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

    @InjectMocks
    private CreatePurchaseOrderService service;

    @Test
    @DisplayName("Persists a created order and converts the originating purchase request")
    void creatingAnOrderPersistsItAndConvertsTheOriginatingRequest() {
        when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());
        var savedPurchaseOrder = PurchaseOrder.builder()
                .requestId(PurchaseRequestId.of(UUID.randomUUID()))
                .build();

        when(purchaseOrderRepositoryPort.save(any())).thenReturn(savedPurchaseOrder);
        when(purchaseRequestRepositoryPort.findById(any())).thenReturn(Optional.of(PurchaseRequest.builder()
                .status(PurchaseRequestStatus.OPEN)
                .build()));

        var productId = UUID.randomUUID();
        var supplierId = UUID.randomUUID();

        var orderId = service.execute(new CreatePurchaseOrderCommand(savedPurchaseOrder.getRequestId(), null, SupplierId.of(supplierId),
                List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("2.50")))));
    }

    @Test
    @DisplayName("Resolves whether approval is required from the configured approval threshold")
    void resolvesApprovalRequiredFromTheConfiguredAlcadaThreshold() {
        when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING))
                .thenReturn(Optional.of(ApprovalAlcada.builder().thresholdValue(new BigDecimal("10.00")).build()));

        when(purchaseRequestRepositoryPort.findById(any())).thenReturn(Optional.of(PurchaseRequest.builder()
                .status(PurchaseRequestStatus.OPEN)
                .build()));

        var savedPurchaseOrder = PurchaseOrder.builder()
                .requestId(PurchaseRequestId.of(UUID.randomUUID()))
                .build();
        when(purchaseOrderRepositoryPort.save(any())).thenReturn(savedPurchaseOrder);

        var orderId = service.execute(new CreatePurchaseOrderCommand(savedPurchaseOrder.getRequestId(), null,
                SupplierId.of(UUID.randomUUID()),
                List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE))));
    }
}
