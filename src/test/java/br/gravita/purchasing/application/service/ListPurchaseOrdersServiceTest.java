package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.inbound.purchasing.PurchaseOrderListItem;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRecord;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.usercases.purchasing.ListPurchaseOrdersService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPurchaseOrdersServiceTest {

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	private PurchaseOrder order(final SupplierId supplier) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()), null, supplier,
				List.of(new PurchaseOrderItem(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("10.50"))), false);
	}

	@Test
	@DisplayName("Lists the orders with the supplier name, looking each supplier up once")
	void listsOrdersWithSupplierNames() {
		final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
		final Supplier supplier = mock(Supplier.class);
		when(supplier.getName()).thenReturn("Fornecedor Beta");
		final LocalDateTime placed = LocalDateTime.of(2026, 10, 1, 9, 0);
		when(purchaseOrderRepositoryPort.findAllNewestFirst()).thenReturn(List.of(
				new PurchaseOrderRecord(order(supplierId), placed), new PurchaseOrderRecord(order(supplierId), placed.minusDays(1))));
		when(supplierRepositoryPort.findById(supplierId)).thenReturn(Optional.of(supplier));

		final List<PurchaseOrderListItem> result = new ListPurchaseOrdersService(purchaseOrderRepositoryPort, supplierRepositoryPort).execute();

		assertThat(result).extracting(PurchaseOrderListItem::supplierName).containsExactly("Fornecedor Beta", "Fornecedor Beta");
		assertThat(result.get(0).createdAt()).isEqualTo(placed);
		assertThat(result.get(0).order().totalValue()).isEqualByComparingTo("21.00");
		verify(supplierRepositoryPort, times(1)).findById(supplierId);
	}

	@Test
	@DisplayName("Keeps an order whose supplier no longer exists, without a name")
	void keepsAnOrderWithAnUnknownSupplier() {
		final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findAllNewestFirst())
				.thenReturn(List.of(new PurchaseOrderRecord(order(supplierId), LocalDateTime.now())));
		when(supplierRepositoryPort.findById(supplierId)).thenReturn(Optional.empty());

		assertThat(new ListPurchaseOrdersService(purchaseOrderRepositoryPort, supplierRepositoryPort).execute())
				.extracting(PurchaseOrderListItem::supplierName).containsExactly((String) null);
	}
}
