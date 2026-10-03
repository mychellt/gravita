package br.gravita.core.usercases.system;

import br.gravita.core.domain.BillingCycle;
import br.gravita.core.domain.CompanyPerson;
import br.gravita.core.domain.PlanFixtures;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.Subscription;
import br.gravita.core.domain.SubscriptionStatus;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.SignupRejectedException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserSignedUp;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.messaging.PublishUserSignedUpPort;
import br.gravita.core.ports.outbound.persistence.CompanyPersonRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SubscriptionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";
	private static final ProfileReference ADMINISTRATOR =
			new ProfileReference(UUID.randomUUID(), SignupService.ADMINISTRATOR_PROFILE_NAME);

	@Mock
	private CompanyPersonRepositoryPort companyRepository;
	@Mock
	private UserRepositoryPort userRepository;
	@Mock
	private ProfileRepositoryPort profileRepository;
	@Mock
	private PlanRepositoryPort planRepository;
	@Mock
	private SubscriptionRepositoryPort subscriptionRepository;
	@Mock
	private PublishUserSignedUpPort publishUserSignedUp;

	private SignupService service;
	private final UUID companyId = UUID.randomUUID();
	private final UUID subscriptionId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new SignupService(companyRepository, userRepository, profileRepository, planRepository,
				subscriptionRepository, publishUserSignedUp);
		lenient().when(profileRepository.findByName(SignupService.ADMINISTRATOR_PROFILE_NAME))
				.thenReturn(Optional.of(ADMINISTRATOR));
		lenient().when(planRepository.findActiveByTier(any(PlanTier.class))).thenAnswer(invocation ->
				Optional.of(PlanFixtures.aPlan().tier(invocation.getArgument(0)).build()));
		lenient().when(companyRepository.save(any(CompanyPerson.class))).thenAnswer(invocation -> {
			CompanyPerson company = invocation.getArgument(0);
			company.setId(companyId);
			return company;
		});
		lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
			Subscription subscription = invocation.getArgument(0);
			subscription.setId(subscriptionId);
			return subscription;
		});
	}

	@Test
	@DisplayName("A valid signup creates the company, an administrator linked to it and an active subscription")
	void shouldCreateCompanyAdministratorAndActiveSubscription() {
		SignupResult result = service.execute(command("silver", "annual"));

		ArgumentCaptor<CompanyPerson> company = ArgumentCaptor.forClass(CompanyPerson.class);
		verify(companyRepository).save(company.capture());
		assertThat(company.getValue().getName()).isEqualTo("Acme Ltda");
		assertThat(company.getValue().getDocument().number()).isEqualTo("11222333000181");
		assertThat(company.getValue().getPhone()).isEqualTo("(11) 91234-5678");

		ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(user.capture());
		assertThat(user.getValue().getEmail()).isEqualTo("ana@acme.com");
		assertThat(user.getValue().getProfileId()).isEqualTo(ADMINISTRATOR.id());
		assertThat(user.getValue().isTwoFactorEnabled()).isTrue();
		assertThat(user.getValue().getCompanyId()).isEqualTo(companyId);
		assertThat(user.getValue().getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);

		ArgumentCaptor<Subscription> subscription = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository).save(subscription.capture());
		assertThat(subscription.getValue().getPlan().getTier()).isEqualTo(PlanTier.SILVER);
		assertThat(subscription.getValue().getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
		assertThat(subscription.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(subscription.getValue().getActivationDate()).isEqualTo(LocalDate.now());
		assertThat(subscription.getValue().getPerson().getId()).isEqualTo(companyId);
		assertThat(subscription.getValue().getPayments()).isEmpty();

		assertThat(result.companyId()).isEqualTo(companyId);
		assertThat(result.subscriptionId()).isEqualTo(subscriptionId);
		assertThat(result.plan()).isEqualTo(PlanTier.SILVER);
		assertThat(result.billingCycle()).isEqualTo(BillingCycle.ANNUAL);
	}

	@Test
	@DisplayName("A completed signup announces the new user, carrying the trimmed e-mail and the name")
	void shouldAnnounceTheNewUser() {
		SignupResult result = service.execute(command("silver", "monthly"));

		ArgumentCaptor<UserSignedUp> event = ArgumentCaptor.forClass(UserSignedUp.class);
		verify(publishUserSignedUp).publish(event.capture());
		assertThat(event.getValue().userId().value()).isEqualTo(result.userId());
		assertThat(event.getValue().name()).isEqualTo("Ana Souza");
		assertThat(event.getValue().email()).isEqualTo("ana@acme.com");
	}

	@Test
	@DisplayName("Without plan and billing it defaults to Silver and Monthly")
	void shouldDefaultToSilverMonthly() {
		SignupResult result = service.execute(command(null, null));

		assertThat(result.plan()).isEqualTo(PlanTier.SILVER);
		assertThat(result.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
		verify(planRepository).findActiveByTier(PlanTier.SILVER);
	}

	@Test
	@DisplayName("Plan and billing slugs are case-insensitive")
	void shouldAcceptSlugsInAnyCase() {
		SignupResult result = service.execute(command(" Gold ", "ANNUAL"));

		assertThat(result.plan()).isEqualTo(PlanTier.GOLD);
		assertThat(result.billingCycle()).isEqualTo(BillingCycle.ANNUAL);
	}

	@Test
	@DisplayName("A duplicate email is rejected on the email field before anything is saved")
	void shouldRejectDuplicateEmail() {
		when(userRepository.existsByEmail("ana@acme.com")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(command("silver", "monthly")))
				.isInstanceOfSatisfying(SignupRejectedException.class,
						e -> assertThat(e.getField()).isEqualTo(SignupRejectedException.EMAIL));
		verify(companyRepository, never()).save(any());
		verify(userRepository, never()).save(any());
		verifyNoInteractions(subscriptionRepository, publishUserSignedUp);
	}

	@Test
	@DisplayName("A duplicate CNPJ is rejected on the cnpj field before anything is saved")
	void shouldRejectDuplicateCnpj() {
		when(companyRepository.existsByDocument("11222333000181")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(command("silver", "monthly")))
				.isInstanceOfSatisfying(SignupRejectedException.class,
						e -> assertThat(e.getField()).isEqualTo(SignupRejectedException.CNPJ));
		verify(companyRepository, never()).save(any());
		verify(userRepository, never()).save(any());
		verifyNoInteractions(subscriptionRepository, publishUserSignedUp);
	}

	@ParameterizedTest
	@ValueSource(strings = {"11.222.333/0001-80", "00.000.000/0000-00", "123", ""})
	@DisplayName("A CNPJ with a wrong check digit is rejected on the cnpj field")
	void shouldRejectInvalidCnpj(String cnpj) {
		SignupCommand command = new SignupCommand("Ana Souza", "ana@acme.com", "s3cret-pass", "Acme Ltda", cnpj,
				null, "silver", "monthly");

		assertThatThrownBy(() -> service.execute(command))
				.isInstanceOfSatisfying(SignupRejectedException.class,
						e -> assertThat(e.getField()).isEqualTo(SignupRejectedException.CNPJ));
		verify(companyRepository, never()).save(any());
	}

	@Test
	@DisplayName("An unknown plan is rejected instead of silently defaulting")
	void shouldRejectUnknownPlan() {
		assertThatThrownBy(() -> service.execute(command("platinum", "monthly")))
				.isInstanceOfSatisfying(SignupRejectedException.class,
						e -> assertThat(e.getField()).isEqualTo(SignupRejectedException.PLAN));
		verify(companyRepository, never()).save(any());
		verify(userRepository, never()).save(any());
		verifyNoInteractions(publishUserSignedUp);
	}

	@Test
	@DisplayName("An unknown billing cycle is rejected instead of silently defaulting")
	void shouldRejectUnknownBilling() {
		assertThatThrownBy(() -> service.execute(command("silver", "weekly")))
				.isInstanceOfSatisfying(SignupRejectedException.class,
						e -> assertThat(e.getField()).isEqualTo(SignupRejectedException.BILLING));
		verify(companyRepository, never()).save(any());
		verifyNoInteractions(publishUserSignedUp);
	}

	@Test
	@DisplayName("A plan with no active catalog entry is rejected")
	void shouldRejectUnavailablePlan() {
		when(planRepository.findActiveByTier(PlanTier.BRONZE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command("bronze", "monthly")))
				.isInstanceOfSatisfying(SignupRejectedException.class,
						e -> assertThat(e.getField()).isEqualTo(SignupRejectedException.PLAN));
		verify(companyRepository, never()).save(any());
	}

	@Test
	@DisplayName("The command's string form never exposes the password")
	void shouldNotExposePasswordInToString() {
		assertThat(command("silver", "monthly").toString()).doesNotContain("s3cret-pass");
	}

	@Test
	@DisplayName("A missing company name is rejected before anything is saved")
	void shouldRejectBlankCompanyName() {
		SignupCommand command = new SignupCommand("Ana Souza", "ana@acme.com", "s3cret-pass", " ", VALID_CNPJ, null,
				"silver", "monthly");

		assertThatThrownBy(() -> service.execute(command)).hasMessageContaining("empresa");
		verify(companyRepository, never()).save(any());
		verify(userRepository, never()).existsByEmail(anyString());
	}

	private static SignupCommand command(String plan, String billing) {
		return new SignupCommand("Ana Souza", " ana@acme.com ", "s3cret-pass", "Acme Ltda", VALID_CNPJ,
				"(11) 91234-5678", plan, billing);
	}
}
