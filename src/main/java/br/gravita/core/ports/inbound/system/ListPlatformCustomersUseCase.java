package br.gravita.core.ports.inbound.system;

import java.util.List;

/** Every company with a Gravita subscription, most recent subscriber first. */
public interface ListPlatformCustomersUseCase {
	List<PlatformCustomerView> execute();
}
