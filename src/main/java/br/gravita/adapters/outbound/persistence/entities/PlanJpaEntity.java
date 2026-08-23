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
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PlanTier tier;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal priceMonthly;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal priceAnnual;

	@ElementCollection
	@CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
	@Column(name = "feature", nullable = false)
	private List<String> features;
}
