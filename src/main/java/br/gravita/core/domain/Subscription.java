package br.gravita.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Subscription extends AbstractDomain {

	private static final int ANNUAL_DAYS = 365;

	private PlanDomain plan;
	private CompanyPerson person;
	private BillingCycle billingCycle;

	@Builder.Default
	private List<Payment> payments = new ArrayList<>();

	private SubscriptionStatus status;
	private LocalDate activationDate;
	private LocalDate expirationDate;

	public static Subscription activate(PlanDomain plan, CompanyPerson person, BillingCycle billingCycle) {
		LocalDate today = LocalDate.now();
		return Subscription.builder()
				.plan(plan)
				.person(person)
				.billingCycle(billingCycle)
				.status(SubscriptionStatus.ACTIVE)
				.activationDate(today)
				.expirationDate(nextExpirationDate(today, billingCycle))
				.build();
	}

	public void registerPayment(Payment payment) {
		payments.add(payment);
		status = SubscriptionStatus.ACTIVE;
		if (billingCycle == BillingCycle.MONTHLY) {
			expirationDate = expirationDate.plusMonths(1);
		}
	}

	public void markPaymentFailure() {
		status = SubscriptionStatus.PAYMENT_FAILURE;
	}

	private static LocalDate nextExpirationDate(LocalDate from, BillingCycle billingCycle) {
		return billingCycle == BillingCycle.ANNUAL ? from.plusDays(ANNUAL_DAYS) : from.plusMonths(1);
	}
}
