package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.ports.inbound.system.PlatformCustomerView;
import br.gravita.core.ports.inbound.system.PlatformPaymentView;
import java.util.List;
import java.util.UUID;

public interface PlatformCustomerQueryPort {

	List<PlatformCustomerView> findAllSubscribers();

	List<PlatformPaymentView> findPaymentsByCompanyId(UUID companyId);
}
