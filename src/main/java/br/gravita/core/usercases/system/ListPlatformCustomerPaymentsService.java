package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.ports.inbound.system.ListPlatformCustomerPaymentsUseCase;
import br.gravita.core.ports.inbound.system.PlatformPaymentView;
import br.gravita.core.ports.outbound.persistence.system.PlatformCustomerQueryPort;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@UseCase
public class ListPlatformCustomerPaymentsService implements ListPlatformCustomerPaymentsUseCase {

	private final PlatformCustomerQueryPort platformCustomerQueryPort;

	public ListPlatformCustomerPaymentsService(final PlatformCustomerQueryPort platformCustomerQueryPort) {
		this.platformCustomerQueryPort = platformCustomerQueryPort;
	}

	@Override
	public List<PlatformPaymentView> execute(final UUID companyId) {
		return platformCustomerQueryPort.findPaymentsByCompanyId(companyId).stream()
				.sorted(Comparator.comparing(PlatformPaymentView::paymentDate).reversed())
				.toList();
	}
}
