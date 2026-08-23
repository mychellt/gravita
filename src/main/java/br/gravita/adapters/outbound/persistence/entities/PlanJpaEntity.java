package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.PlanTier;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanJpaEntity {

	@Id
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

	protected PlanJpaEntity() {
	}

	public PlanJpaEntity(UUID id, String name, PlanTier tier, BigDecimal priceMonthly, BigDecimal priceAnnual, List<String> features) {
		this.id = id;
		this.name = name;
		this.tier = tier;
		this.priceMonthly = priceMonthly;
		this.priceAnnual = priceAnnual;
		this.features = features;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public PlanTier getTier() {
		return tier;
	}

	public BigDecimal getPriceMonthly() {
		return priceMonthly;
	}

	public BigDecimal getPriceAnnual() {
		return priceAnnual;
	}

	public List<String> getFeatures() {
		return features;
	}
}
