package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.system.SignupRejectedException;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.messaging.NotifyUserRegistrationProducerPort;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.ports.outbound.persistence.CompanyPersonRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SubscriptionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@AllArgsConstructor
@UseCase
public class SignupService implements SignupUseCase {

    static final String ADMINISTRATOR_PROFILE_NAME = "Administrator";
    private static final PlanTier DEFAULT_PLAN = PlanTier.SILVER;
    private static final BillingCycle DEFAULT_BILLING = BillingCycle.MONTHLY;

    private final CompanyPersonRepositoryPort companyRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;
    private final PlanRepositoryPort planRepositoryPort;
    private final SubscriptionRepositoryPort subscriptionRepositoryPort;
    private final NotifyUserRegistrationProducerPort notifyUserRegistrationProducerPort;

    @Override
    @Transactional
    public SignupResult execute(final Context context) {
        final SignupCommand command = context.getData(SignupCommand.class);
        final var tier = parsePlan(command.plan());
        final var billingCycle = parseBilling(command.billing());
        final var cnpj = parseCnpj(command.cnpj());
        final var companyName = requireText(command.companyName(), "Informe o nome da empresa.");
        final var email = command.email() == null ? null : command.email().strip();

        if (email != null && userRepositoryPort.existsByEmail(email)) {
            throw new SignupRejectedException(SignupRejectedException.EMAIL, "Este e-mail já está cadastrado.");
        }
        if (companyRepositoryPort.existsByDocument(cnpj.number())) {
            throw new SignupRejectedException(SignupRejectedException.CNPJ, "Este CNPJ já está cadastrado.");
        }

        final var plan = planRepositoryPort.findActiveByTier(tier)
                .orElseThrow(() -> new SignupRejectedException(SignupRejectedException.PLAN,
                        "O plano " + tier.name().toLowerCase(Locale.ROOT) + " não está disponível."));

        final var administrator = profileRepositoryPort.findByName(ADMINISTRATOR_PROFILE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Profile '" + ADMINISTRATOR_PROFILE_NAME + "' is not seeded; signup cannot assign it"));

        final var company = companyRepositoryPort.save(CompanyPerson.builder()
                .name(companyName)
                .document(cnpj)
                .phone(command.phone())
                .active(true)
                .build());

        final var user = User.signUp(command.fullName(), email, command.rawPassword(), administrator, company.getId());
        final var userId = userRepositoryPort.save(user).getId();

        final var subscription = Subscription.request(plan, company, billingCycle);
        subscription.activate();
        Subscription saved = subscriptionRepositoryPort.save(subscription);

        notifyUserRegistrationProducerPort.execute(new Context(NotifyUserRegistrationMessage.builder()
                .username(user.getName())
                .recipient(user.getEmail())
                .tenantId(UUID.randomUUID())
                .build()));

        return new SignupResult(userId.value(), company.getId(), saved.getId(), tier, billingCycle,
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
