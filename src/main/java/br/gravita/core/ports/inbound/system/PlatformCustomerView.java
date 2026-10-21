package br.gravita.core.ports.inbound.system;

import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.util.UUID;

/** A company that subscribed to a Gravita plan, as the platform administration sees it. */
public record PlatformCustomerView(UUID companyId, String cnpj, String name, String email, String phone, PlanTier planTier,
		String planName, SubscriptionStatus status, LocalDate activationDate, LocalDate expirationDate) {
}
