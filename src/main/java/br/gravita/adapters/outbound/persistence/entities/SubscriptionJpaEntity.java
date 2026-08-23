package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.BillingCycle;
import br.gravita.core.domain.SubscriptionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscriptions")
public class SubscriptionJpaEntity extends AbstractEntity<UUID> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private PlanJpaEntity plan;

	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	private CompanyPersonJpaEntity person;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private BillingCycle billingCycle;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SubscriptionStatus status;

	@Column(nullable = false)
	private LocalDate activationDate;

	@Column(nullable = false)
	private LocalDate expirationDate;

	@OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PaymentJpaEntity> payments;
}
