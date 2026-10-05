package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.ports.inbound.sales.ApproveSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.ApproveSalesOrderUseCase;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.sales.ReserveStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@UseCase
public class ApproveSalesOrderService implements ApproveSalesOrderUseCase {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;
	private final ReserveStockPort reserveStockPort;

	@Override
	@Transactional
	public SalesOrderView execute(final ApproveSalesOrderCommand command) {
		final SalesOrder order = salesOrderRepositoryPort.findById(SalesOrderId.of(command.orderId()))
				.orElseThrow(() -> new SalesOrderNotFoundException(command.orderId()));

		final Optional<ApprovalAlcada> alcada = approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES);
		if (alcada.filter(a -> exceedsAlcada(order, a)).isPresent()) {
			requireElevatedApprover(command.approvedBy(), alcada.get());
		}

		final SalesOrder approved = order.approve(command.approvedBy(), alcada.map(ApprovalAlcada::getId).orElse(null));

		for (final SalesOrderItem item : order.getItems()) {
			reserveStockPort.reserve(new ReserveStockPort.ReserveStockForOrderCommand(order.getId().value(),
					item.productOrServiceId(), item.quantity()));
		}

		final SalesOrder saved = salesOrderRepositoryPort.save(approved);
		return SalesOrderView.from(saved);
	}

	private boolean exceedsAlcada(final SalesOrder order, final ApprovalAlcada alcada) {
		final boolean exceedsValue = alcada.getThresholdValue() != null
				&& order.totalValue().compareTo(alcada.getThresholdValue()) >= 0;
		final boolean exceedsDiscount = alcada.getThresholdDiscountPercent() != null
				&& order.discountPercent().compareTo(alcada.getThresholdDiscountPercent()) >= 0;
		return exceedsValue || exceedsDiscount;
	}

	private void requireElevatedApprover(final UUID approvedBy, final ApprovalAlcada alcada) {
		final User approver = userRepositoryPort.findById(UserId.of(approvedBy))
				.orElseThrow(() -> new UserNotFoundException(approvedBy));
		if (!approver.getProfileId().equals(alcada.getApproverProfileId())) {
			throw new BusinessRuleException(
					"Approving this order exceeds the sales alcada and requires an approver with the elevated profile");
		}
	}
}
