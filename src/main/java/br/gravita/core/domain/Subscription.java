package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
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
	private Company company;
	private BillingCycle billingCycle;

	@Builder.Default
	private List<Payment> payments = new ArrayList<>();

	private SubscriptionStatus status;
	private LocalDate activationDate;
	private LocalDate expirationDate;

	public static Subscription request(final PlanDomain plan, final Company company, final BillingCycle billingCycle) {
		return Subscription.builder()
				.plan(plan)
				.company(company)
				.billingCycle(billingCycle)
				.status(SubscriptionStatus.PENDING)
				.build();
	}

	public void activate() {
		if (status != SubscriptionStatus.PENDING) {
			throw new BusinessRuleException("Only a pending subscription can be activated");
		}
		final LocalDate today = LocalDate.now();
		this.activationDate = today;
		this.expirationDate = nextExpirationDate(today, billingCycle);
		this.status = SubscriptionStatus.ACTIVE;
	}

	public void registerPayment(final Payment payment) {
		payments.add(payment);
		status = SubscriptionStatus.ACTIVE;
		if (billingCycle == BillingCycle.MONTHLY) {
			expirationDate = expirationDate.plusMonths(1);
		}
	}

	public void markPaymentFailure() {
		status = SubscriptionStatus.PAYMENT_FAILURE;
	}

	private static LocalDate nextExpirationDate(final LocalDate from, final BillingCycle billingCycle) {
		return billingCycle == BillingCycle.ANNUAL ? from.plusDays(ANNUAL_DAYS) : from.plusMonths(1);
	}
}
