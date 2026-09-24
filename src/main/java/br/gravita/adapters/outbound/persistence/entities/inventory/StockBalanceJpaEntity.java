package br.gravita.adapters.outbound.persistence.entities.inventory;

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
@Table(name = "stock_balances")
public class StockBalanceJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "warehouse_id", nullable = false)
	private UUID warehouseId;

	@Column(name = "on_hand", nullable = false, precision = 14, scale = 4)
	private BigDecimal onHand;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal reserved;

	@Column(name = "in_transit", nullable = false, precision = 14, scale = 4)
	private BigDecimal inTransit;

	@Column(name = "average_cost", nullable = false, precision = 14, scale = 4)
	private BigDecimal averageCost;
}
