package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.sales.OpportunityStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "opportunities")
public class OpportunityJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "estimated_value", nullable = false, precision = 14, scale = 2)
	private BigDecimal estimatedValue;

	@Column(nullable = false)
	private Integer probability;

	@Column(name = "expected_close_date", nullable = false)
	private LocalDate expectedCloseDate;

	@Column(nullable = false)
	private UUID owner;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OpportunityStage stage;
}
