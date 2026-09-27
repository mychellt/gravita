package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "salesperson_targets")
public class SalespersonTargetJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "salesperson_id", nullable = false)
	private UUID salespersonId;

	@Column(name = "target_month", nullable = false, length = 7)
	private String month;

	@Column(name = "value_target", nullable = false, precision = 14, scale = 4)
	private BigDecimal valueTarget;

	@Column(name = "order_count_target", nullable = false)
	private int orderCountTarget;
}
