package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseOrderPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseReceiptPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort.RegisterStockEntryCommand;
import br.gravita.core.usercases.purchasing.ConfirmPurchaseReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@ExtendWith(MockitoExtension.class)
@Import({PurchaseOrderRepositoryAdapter.class, PurchaseOrderPersistenceMapperImpl.class,
		PurchaseReceiptRepositoryAdapter.class, PurchaseReceiptPersistenceMapperImpl.class})
class ConfirmPurchaseReceiptIntegrationTest {

	@Autowired
	private PurchaseOrderRepositoryAdapter purchaseOrderRepositoryAdapter;

	@Autowired
	private PurchaseReceiptRepositoryAdapter purchaseReceiptRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Mock
	private RegisterStockEntryPort registerStockEntryPort;

	@Mock
	private GeneratePayableFromReceiptPort generatePayableFromReceiptPort;

	private ConfirmPurchaseReceiptService service;

	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ConfirmPurchaseReceiptService(purchaseReceiptRepositoryAdapter, purchaseOrderRepositoryAdapter,
				registerStockEntryPort, generatePayableFromReceiptPort);
	}

	@Test
	void confirmingAFullyReceivedReceiptPersistsItAndClosesTheOrder() {
		PurchaseOrderId orderId = persistOpenOrder(BigDecimal.TEN);
		PurchaseReceiptId receiptId = persistConferencedReceipt(orderId, BigDecimal.TEN);

		service.execute(new ConfirmPurchaseReceiptCommand(receiptId));
		flushAndClear();

		PurchaseReceipt persistedReceipt = purchaseReceiptRepositoryAdapter.findById(receiptId).orElseThrow();
		assertThat(persistedReceipt.getStatus()).isEqualTo(PurchaseReceiptStatus.CONFIRMED);

		PurchaseOrder persistedOrder = purchaseOrderRepositoryAdapter.findById(orderId).orElseThrow();
		assertThat(persistedOrder.getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);

		ArgumentCaptor<RegisterStockEntryCommand> stockEntry = ArgumentCaptor.forClass(RegisterStockEntryCommand.class);
		verify(registerStockEntryPort).registerEntry(stockEntry.capture());
		assertThat(stockEntry.getValue().productId()).isEqualTo(productId);
	}

	private PurchaseOrderId persistOpenOrder(BigDecimal orderedQty) {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, orderedQty, new BigDecimal("5.00"))), false);
		return purchaseOrderRepositoryAdapter.save(order).getId();
	}

	private PurchaseReceiptId persistConferencedReceipt(PurchaseOrderId orderId, BigDecimal receivedQty) {
		var receipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, receivedQty, receivedQty)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("50.00"), LocalDate.now().plusDays(30))));
		return purchaseReceiptRepositoryAdapter.save(receipt).getId();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
