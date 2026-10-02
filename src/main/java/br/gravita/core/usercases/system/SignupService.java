package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.BillingCycle;
import br.gravita.core.domain.CompanyPerson;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.Subscription;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.SignupRejectedException;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.outbound.persistence.CompanyPersonRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SubscriptionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Self-service signup: creates the tenant company, its administrator and an active subscription in one
 * transaction, so any rejection leaves nothing behind.
 */
@UseCase
public class SignupService implements SignupUseCase {

	static final UUID ADMINISTRATOR_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final PlanTier DEFAULT_PLAN = PlanTier.SILVER;
	private static final BillingCycle DEFAULT_BILLING = BillingCycle.MONTHLY;

	private final CompanyPersonRepositoryPort companyRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;
	private final PlanRepositoryPort planRepositoryPort;
	private final SubscriptionRepositoryPort subscriptionRepositoryPort;

	public SignupService(CompanyPersonRepositoryPort companyRepositoryPort, UserRepositoryPort userRepositoryPort,
			ProfileRepositoryPort profileRepositoryPort, PlanRepositoryPort planRepositoryPort,
			SubscriptionRepositoryPort subscriptionRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
		this.planRepositoryPort = planRepositoryPort;
		this.subscriptionRepositoryPort = subscriptionRepositoryPort;
	}

	@Override
	@Transactional
	public SignupResult execute(SignupCommand command) {
		PlanTier tier = parsePlan(command.plan());
		BillingCycle billingCycle = parseBilling(command.billing());
		Document cnpj = parseCnpj(command.cnpj());
		String companyName = requireText(command.companyName(), "Informe o nome da empresa.");
		String email = command.email() == null ? null : command.email().strip();

		if (email != null && userRepositoryPort.existsByEmail(email)) {
			throw new SignupRejectedException(SignupRejectedException.EMAIL, "Este e-mail já está cadastrado.");
		}
		if (companyRepositoryPort.existsByDocument(cnpj.number())) {
			throw new SignupRejectedException(SignupRejectedException.CNPJ, "Este CNPJ já está cadastrado.");
		}
		PlanDomain plan = planRepositoryPort.findActiveByTier(tier)
				.orElseThrow(() -> new SignupRejectedException(SignupRejectedException.PLAN,
						"O plano " + tier.name().toLowerCase(Locale.ROOT) + " não está disponível."));
		ProfileReference administrator = profileRepositoryPort.findById(ADMINISTRATOR_PROFILE_ID)
				.orElseThrow(() -> new IllegalStateException(
						"Administrator profile " + ADMINISTRATOR_PROFILE_ID + " is not seeded; signup cannot assign it"));

		CompanyPerson company = companyRepositoryPort.save(CompanyPerson.builder()
				.name(companyName)
				.document(cnpj)
				.phone(command.phone())
				.active(true)
				.build());

		User user = User.register(command.fullName(), email, command.rawPassword(), administrator, company.getId());
		UUID userId = userRepositoryPort.save(user).getId().value();

		Subscription subscription = Subscription.request(plan, company, billingCycle);
		subscription.activate();
		Subscription saved = subscriptionRepositoryPort.save(subscription);

		return new SignupResult(userId, company.getId(), saved.getId(), tier, billingCycle,
				saved.getActivationDate(), saved.getExpirationDate());
	}

	private static PlanTier parsePlan(String slug) {
		if (slug == null || slug.isBlank()) {
			return DEFAULT_PLAN;
		}
		try {
			return PlanTier.valueOf(slug.strip().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new SignupRejectedException(SignupRejectedException.PLAN, "Plano desconhecido: " + slug);
		}
	}

	private static BillingCycle parseBilling(String slug) {
		if (slug == null || slug.isBlank()) {
			return DEFAULT_BILLING;
		}
		try {
			return BillingCycle.valueOf(slug.strip().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new SignupRejectedException(SignupRejectedException.BILLING, "Ciclo de cobrança desconhecido: " + slug);
		}
	}

	private static Document parseCnpj(String value) {
		try {
			return Document.cnpj(value);
		} catch (BusinessRuleException e) {
			throw new SignupRejectedException(SignupRejectedException.CNPJ, "CNPJ inválido. Verifique e tente novamente.");
		}
	}

	private static String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(message);
		}
		return value.strip();
	}
}
