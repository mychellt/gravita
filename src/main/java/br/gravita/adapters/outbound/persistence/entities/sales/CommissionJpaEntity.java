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
@Table(name = "commissions")
public class CommissionJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "salesperson_id", nullable = false)
	private UUID salespersonId;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "sales_order_id", nullable = false)
	private UUID salesOrderId;

	@Column(nullable = false, precision = 7, scale = 4)
	private BigDecimal rate;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal amount;
}
