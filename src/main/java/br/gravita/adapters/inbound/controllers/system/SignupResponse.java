package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.BillingCycle;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.usercases.system.SignupResult;

import java.time.LocalDate;
import java.util.UUID;

public record SignupResponse(UUID userId, UUID companyId, UUID subscriptionId, String email, PlanTier plan,
		BillingCycle billingCycle, LocalDate activationDate, LocalDate expirationDate) {

	public static SignupResponse from(final SignupResult result, final String email) {
		return new SignupResponse(result.userId(), result.companyId(), result.subscriptionId(), email, result.plan(),
				result.billingCycle(), result.activationDate(), result.expirationDate());
	}
}
