package br.gravita.core.usercases.system;

import br.gravita.core.domain.BillingCycle;
import br.gravita.core.domain.PlanTier;

import java.time.LocalDate;
import java.util.UUID;

public record SignupResult(UUID userId, UUID companyId, UUID subscriptionId, PlanTier plan, BillingCycle billingCycle,
		LocalDate activationDate, LocalDate expirationDate) {
}
