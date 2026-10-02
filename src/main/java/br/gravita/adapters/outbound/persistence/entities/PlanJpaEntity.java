package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.PlanTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plans")
public class PlanJpaEntity extends AbstractEntity<UUID> {
	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PlanTier tier;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal priceMonthly;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal priceAnnual;

	@Column(nullable = false)
	private boolean featured;

	@Embedded
	private PlanLimitsEmbeddable limits;

	@Embedded
	private PlanSupportEmbeddable support;

	@ElementCollection
	@CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
	@OrderBy("displayOrder ASC")
	private List<PlanFeatureEmbeddable> features;

	/** Hibernate leaves an embedded object null when all its columns are null, i.e. when every limit is unlimited. */
	public PlanLimitsEmbeddable getLimits() {
		return limits == null ? new PlanLimitsEmbeddable() : limits;
	}
}
