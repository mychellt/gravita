package br.gravita.purchasing.application.service;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.usercases.purchasing.CreatePurchaseOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePurchaseOrderServiceTest {

    @Mock
    private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

    @Mock
    private QuotationRepositoryPort quotationRepositoryPort;

    @Mock
    private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

    @Mock
    private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

    @InjectMocks
    private CreatePurchaseOrderService service;

    @Test
    @DisplayName("Creates an order from an open request and marks the request as converted")
    void createsAnOrderFromAnOpenRequestAndConvertsIt() {
        PurchaseRequestId requestId = openRequest();

        when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());

        SupplierId supplierId = SupplierId.of(UUID.randomUUID());
        List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
                new BigDecimal("5.00")));

        var id = service.execute(new CreatePurchaseOrderCommand(requestId, null, supplierId, items));

        assertThat(id).isNotNull();
        ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getRequestId()).isEqualTo(requestId);
        assertThat(savedOrder.getValue().getSupplierId()).isEqualTo(supplierId);
        assertThat(savedOrder.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
        assertThat(savedOrder.getValue().isApprovalRequired()).isFalse();

        ArgumentCaptor<PurchaseRequest> savedRequest = ArgumentCaptor.forClass(PurchaseRequest.class);
        verify(purchaseRequestRepositoryPort).save(savedRequest.capture());
        assertThat(savedRequest.getValue().getStatus()).isEqualTo(PurchaseRequestStatus.CONVERTED);
    }

    @Test
    @DisplayName("Creates an order from a quoted request pricing items from the selected supplier's response")
    void createsAnOrderFromAQuotedRequestPricingItemsFromTheSelectedSuppliersResponse() {
        PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
        UUID productId = UUID.randomUUID();
        when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
                PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
                        List.of(new PurchaseRequestItem(productId, BigDecimal.ONE)), UUID.randomUUID(),
                        PurchaseRequestStatus.QUOTED)));
        when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());

        UUID quotationId = UUID.randomUUID();
        SupplierId supplierId = SupplierId.of(UUID.randomUUID());
        Quotation quotation = Quotation.send(QuotationId.of(quotationId), requestId,
                        List.of(new QuotationItem(productId, BigDecimal.ONE)), List.of(supplierId))
                .registerResponse(supplierId, List.of(new QuotationItemPrice(productId, new BigDecimal("9.99"))),
                        LocalDate.now().plusDays(5));
        when(quotationRepositoryPort.findById(QuotationId.of(quotationId))).thenReturn(Optional.of(quotation));

        List<PurchaseOrderItem> commandItems = List.of(new PurchaseOrderItem(productId, BigDecimal.ONE,
                BigDecimal.TEN));

        service.execute(new CreatePurchaseOrderCommand(requestId, quotationId, supplierId, commandItems));

        ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getQuotationId()).isEqualTo(quotationId);
        assertThat(savedOrder.getValue().getItems()).extracting(PurchaseOrderItem::unitPrice)
                .containsExactly(new BigDecimal("9.99"));
    }

    @Test
    @DisplayName("Rejects an order for a purchase request that does not exist")
    void rejectsARequestThatDoesNotExist() {
        PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
        when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());
        List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN));

        assertThatThrownBy(() -> service.execute(
                new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items)))
                .isInstanceOf(PurchaseRequestNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects an order for a purchase request that is already converted")
    void rejectsARequestThatIsAlreadyConverted() {
        PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
        when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
                PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
                        List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID(),
                        PurchaseRequestStatus.CONVERTED)));
        List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN));

        assertThatThrownBy(() -> service.execute(
                new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("OPEN or QUOTED");
        verify(purchaseOrderRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Requires approval when the order total meets the configured threshold")
    void requiresApprovalWhenTheOrderTotalMeetsTheConfiguredThreshold() {
        PurchaseRequestId requestId = openRequest();
        when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.of(
                ApprovalAlcada.builder().thresholdValue(new BigDecimal("100.00")).build()));
        List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
                new BigDecimal("10.00")));

        service.execute(new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items));

        ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
        assertThat(savedOrder.getValue().isApprovalRequired()).isTrue();
    }

    @Test
    @DisplayName("Skips approval when the order total is below the configured threshold")
    void skipsApprovalWhenTheOrderTotalIsBelowTheConfiguredThreshold() {
        PurchaseRequestId requestId = openRequest();
        when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.of(
                ApprovalAlcada.builder().thresholdValue(new BigDecimal("1000.00")).build()));
        List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE,
                new BigDecimal("10.00")));

        service.execute(new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items));

        ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
        assertThat(savedOrder.getValue().isApprovalRequired()).isFalse();
    }

    private PurchaseRequestId openRequest() {
        PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
        when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
                PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
                        List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID(),
                        PurchaseRequestStatus.OPEN)));
        return requestId;
    }
}
