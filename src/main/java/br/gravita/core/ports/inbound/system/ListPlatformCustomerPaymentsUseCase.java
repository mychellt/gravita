package br.gravita.core.ports.inbound.system;

import java.util.List;
import java.util.UUID;

/** The payments received for a company's Gravita subscription, latest first. */
public interface ListPlatformCustomerPaymentsUseCase {
	List<PlatformPaymentView> execute(UUID companyId);
}
