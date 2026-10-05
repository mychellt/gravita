package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.ports.inbound.sales.ApproveSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.sales.ReserveStockPort;
import br.gravita.core.usercases.sales.ApproveSalesOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApproveSalesOrderServiceTest {

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Mock
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private ReserveStockPort reserveStockPort;

	@InjectMocks
	private ApproveSalesOrderService service;

	@Test
	@DisplayName("Approves a draft order below the approval limit and reserves stock for every item")
	void approvesADraftOrderBelowTheAlcadaAndReservesStockForEveryItem() {
		final UUID productA = UUID.randomUUID();
		final UUID productB = UUID.randomUUID();
		final SalesOrder order = draftOrder(
				List.of(item(productA, new BigDecimal("2"), new BigDecimal("10.00"), BigDecimal.ZERO),
						item(productB, BigDecimal.ONE, new BigDecimal("5.00"), BigDecimal.ZERO)));
		final UUID approvedBy = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.empty());
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final SalesOrderView view = service.execute(new ApproveSalesOrderCommand(order.getId().value(), approvedBy));

		assertThat(view.status()).isEqualTo(SalesOrderStatus.APPROVED);
		assertThat(view.approvedBy()).isEqualTo(approvedBy);
		assertThat(view.alcadaId()).isNull();

		final ArgumentCaptor<SalesOrder> savedOrder = ArgumentCaptor.forClass(SalesOrder.class);
		verify(salesOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
		assertThat(savedOrder.getValue().getApprovedBy()).isEqualTo(approvedBy);

		final ArgumentCaptor<ReserveStockPort.ReserveStockForOrderCommand> reservations = ArgumentCaptor
				.forClass(ReserveStockPort.ReserveStockForOrderCommand.class);
		verify(reserveStockPort, times(2)).reserve(reservations.capture());
		assertThat(reservations.getAllValues()).extracting(ReserveStockPort.ReserveStockForOrderCommand::productOrServiceId)
				.containsExactly(productA, productB);
		assertThat(reservations.getAllValues()).allSatisfy(
				reservation -> assertThat(reservation.orderId()).isEqualTo(order.getId().value()));

		verify(userRepositoryPort, never()).findById(any());
	}

	@Test
	@DisplayName("Rejects approving an order that does not exist")
	void rejectsApprovingAnOrderThatDoesNotExist() {
		final UUID orderId = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ApproveSalesOrderCommand(orderId, UUID.randomUUID())))
				.isInstanceOf(SalesOrderNotFoundException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(reserveStockPort, never()).reserve(any());
	}

	@Test
	@DisplayName("Rejects approving an order that is not in DRAFT")
	void rejectsApprovingAnOrderThatIsNotDraft() {
		final SalesOrder approvedOrder = draftOrder(List.of(item(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN,
				BigDecimal.ZERO))).approve(UUID.randomUUID(), null);
		when(salesOrderRepositoryPort.findById(approvedOrder.getId())).thenReturn(Optional.of(approvedOrder));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service
				.execute(new ApproveSalesOrderCommand(approvedOrder.getId().value(), UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(reserveStockPort, never()).reserve(any());
	}

	@Test
	@DisplayName("An order above the value approval limit can be approved by an approver with the elevated profile")
	void ordersExceedingTheValueAlcadaRequireAnApproverWithTheElevatedProfile() {
		final SalesOrder order = draftOrder(
				List.of(item(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("50.00"), BigDecimal.ZERO)));
		final UUID elevatedProfileId = UUID.randomUUID();
		final UUID approvedBy = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.of(
				ApprovalAlcada.builder().id(UUID.randomUUID()).thresholdValue(new BigDecimal("100.00"))
						.approverProfileId(elevatedProfileId).build()));
		when(userRepositoryPort.findById(UserId.of(approvedBy))).thenReturn(
				Optional.of(User.builder().id(UserId.of(approvedBy)).profileId(elevatedProfileId).build()));
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final SalesOrderView view = service.execute(new ApproveSalesOrderCommand(order.getId().value(), approvedBy));

		assertThat(view.status()).isEqualTo(SalesOrderStatus.APPROVED);
		assertThat(view.approvedBy()).isEqualTo(approvedBy);
	}

	@Test
	@DisplayName("Rejects approving an order above the approval limit when the approver lacks the elevated profile")
	void rejectsApprovingAnOrderExceedingTheAlcadaWhenTheApproverLacksTheElevatedProfile() {
		final SalesOrder order = draftOrder(
				List.of(item(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("50.00"), BigDecimal.ZERO)));
		final UUID approvedBy = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.of(
				ApprovalAlcada.builder().id(UUID.randomUUID()).thresholdValue(new BigDecimal("100.00"))
						.approverProfileId(UUID.randomUUID()).build()));
		when(userRepositoryPort.findById(UserId.of(approvedBy))).thenReturn(
				Optional.of(User.builder().id(UserId.of(approvedBy)).profileId(UUID.randomUUID()).build()));

		assertThatThrownBy(() -> service.execute(new ApproveSalesOrderCommand(order.getId().value(), approvedBy)))
				.isInstanceOf(BusinessRuleException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(reserveStockPort, never()).reserve(any());
	}

	@Test
	@DisplayName("An order above the discount approval limit requires an approver with the elevated profile")
	void ordersExceedingTheDiscountAlcadaRequireAnApproverWithTheElevatedProfile() {
		final SalesOrder order = draftOrder(
				List.of(item(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("30.00"))));
		final UUID approvedBy = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.of(
				ApprovalAlcada.builder().id(UUID.randomUUID()).thresholdDiscountPercent(new BigDecimal("20.00"))
						.approverProfileId(UUID.randomUUID()).build()));
		when(userRepositoryPort.findById(UserId.of(approvedBy))).thenReturn(
				Optional.of(User.builder().id(UserId.of(approvedBy)).profileId(UUID.randomUUID()).build()));

		assertThatThrownBy(() -> service.execute(new ApproveSalesOrderCommand(order.getId().value(), approvedBy)))
				.isInstanceOf(BusinessRuleException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects approving an order when the approver does not exist")
	void rejectsApprovingWhenTheApproverDoesNotExist() {
		final SalesOrder order = draftOrder(
				List.of(item(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("50.00"), BigDecimal.ZERO)));
		final UUID approvedBy = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.of(
				ApprovalAlcada.builder().id(UUID.randomUUID()).thresholdValue(new BigDecimal("100.00"))
						.approverProfileId(UUID.randomUUID()).build()));
		when(userRepositoryPort.findById(UserId.of(approvedBy))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ApproveSalesOrderCommand(order.getId().value(), approvedBy)))
				.isInstanceOf(UserNotFoundException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
	}

	private static SalesOrder draftOrder(final List<SalesOrderItem> items) {
		return SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), items);
	}

	private static SalesOrderItem item(final UUID productOrServiceId, final BigDecimal quantity, final BigDecimal unitPrice,
			final BigDecimal discount) {
		return new SalesOrderItem(productOrServiceId, quantity, unitPrice, discount);
	}
}
