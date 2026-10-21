package br.gravita.adapters.inbound.controllers.system;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.system.ListPlatformCustomerPaymentsUseCase;
import br.gravita.core.ports.inbound.system.ListPlatformCustomersUseCase;
import br.gravita.core.ports.inbound.system.PlatformCustomerView;
import br.gravita.core.ports.inbound.system.PlatformPaymentView;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only view of the companies subscribed to Gravita plans, for the platform administration screens. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/customers")
public class PlatformCustomerController {

	private final ListPlatformCustomersUseCase listPlatformCustomersUseCase;
	private final ListPlatformCustomerPaymentsUseCase listPlatformCustomerPaymentsUseCase;

	@GetMapping
	public List<PlatformCustomerView> findAll(@AuthenticatedUser final UserId callerId) {
		return listPlatformCustomersUseCase.execute();
	}

	@GetMapping("/{companyId}/payments")
	public List<PlatformPaymentView> findPayments(@AuthenticatedUser final UserId callerId,
			@PathVariable final UUID companyId) {
		return listPlatformCustomerPaymentsUseCase.execute(companyId);
	}
}
