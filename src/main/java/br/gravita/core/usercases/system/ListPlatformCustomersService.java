package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.ports.inbound.system.ListPlatformCustomersUseCase;
import br.gravita.core.ports.inbound.system.PlatformCustomerView;
import br.gravita.core.ports.outbound.persistence.system.PlatformCustomerQueryPort;
import java.util.List;

@UseCase
public class ListPlatformCustomersService implements ListPlatformCustomersUseCase {

	private final PlatformCustomerQueryPort platformCustomerQueryPort;

	public ListPlatformCustomersService(final PlatformCustomerQueryPort platformCustomerQueryPort) {
		this.platformCustomerQueryPort = platformCustomerQueryPort;
	}

	@Override
	public List<PlatformCustomerView> execute() {
		return platformCustomerQueryPort.findAllSubscribers();
	}
}
