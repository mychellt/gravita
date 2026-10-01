package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseOrderPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseReceiptPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseReturnPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand.ReturnedItem;
import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort.ReversePayableFromReturnCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort.ReverseStockEntryCommand;
import br.gravita.core.usercases.purchasing.ReturnToSupplierService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
		PurchaseReceiptRepositoryAdapter.class, PurchaseReceiptPersistenceMapperImpl.class,
		PurchaseReturnRepositoryAdapter.class, PurchaseReturnPersistenceMapperImpl.class})
class ReturnToSupplierIntegrationTest {

	@Autowired
	private PurchaseOrderRepositoryAdapter purchaseOrderRepositoryAdapter;

	@Autowired
	private PurchaseReceiptRepositoryAdapter purchaseReceiptRepositoryAdapter;

	@Autowired
	private PurchaseReturnRepositoryAdapter purchaseReturnRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Mock
	private ReverseStockEntryPort reverseStockEntryPort;

	@Mock
	private ReversePayableFromReturnPort reversePayableFromReturnPort;

	@Mock
	private IssuePurchaseReturnNfePort issuePurchaseReturnNfePort;

	private ReturnToSupplierService service;

	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ReturnToSupplierService(purchaseReceiptRepositoryAdapter, purchaseOrderRepositoryAdapter,
				purchaseReturnRepositoryAdapter, reverseStockEntryPort, reversePayableFromReturnPort,
				issuePurchaseReturnNfePort);
	}

	@Test
	@DisplayName("Persists a full return of a confirmed receipt as total and reverses the stock and payable")
	void returningAConfirmedReceiptInFullPersistsItAsTotalAndReversesStockAndPayable() {
		when(issuePurchaseReturnNfePort.issueReturnNfe(any())).thenReturn("nfe-ref-123");

		PurchaseOrderId orderId = persistOpenOrder(BigDecimal.TEN);
		PurchaseReceiptId receiptId = persistConfirmedReceipt(orderId, BigDecimal.TEN);

		PurchaseReturnId returnId = service
				.execute(new ReturnToSupplierCommand(receiptId, List.of(new ReturnedItem(productId, BigDecimal.TEN))));
		flushAndClear();

		PurchaseReturn persisted = purchaseReturnRepositoryAdapter.findById(returnId).orElseThrow();
		assertThat(persisted.getReceiptId()).isEqualTo(receiptId);
		assertThat(persisted.isTotal()).isTrue();
		assertThat(persisted.getReturnNfeRef()).isEqualTo("nfe-ref-123");
		assertThat(persisted.getItems()).hasSize(1);
		assertThat(persisted.getItems().get(0).productId()).isEqualTo(productId);
		assertThat(persisted.getItems().get(0).quantity()).isEqualByComparingTo(BigDecimal.TEN);

		ArgumentCaptor<ReverseStockEntryCommand> stockReversal = ArgumentCaptor.forClass(ReverseStockEntryCommand.class);
		verify(reverseStockEntryPort).reverseEntry(stockReversal.capture());
		assertThat(stockReversal.getValue().productId()).isEqualTo(productId);

		ArgumentCaptor<ReversePayableFromReturnCommand> payableReversal =
				ArgumentCaptor.forClass(ReversePayableFromReturnCommand.class);
		verify(reversePayableFromReturnPort).reversePayable(payableReversal.capture());
		assertThat(payableReversal.getValue().amount()).isEqualByComparingTo("50.00");
	}

	private PurchaseOrderId persistOpenOrder(BigDecimal orderedQty) {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, orderedQty, new BigDecimal("5.00"))), false);
		return purchaseOrderRepositoryAdapter.save(order).getId();
	}

	private PurchaseReceiptId persistConfirmedReceipt(PurchaseOrderId orderId, BigDecimal receivedQty) {
		var receipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, receivedQty, receivedQty)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("50.00"), LocalDate.now().plusDays(30))))
				.confirm();
		return purchaseReceiptRepositoryAdapter.save(receipt).getId();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
