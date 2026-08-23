package br.gravita.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PlanDomain extends AbstractDomain {
	private String name;
	private PlanTier tier;
	private BigDecimal priceMonthly;
	private BigDecimal priceAnnual;
	private List<String> features;
}
