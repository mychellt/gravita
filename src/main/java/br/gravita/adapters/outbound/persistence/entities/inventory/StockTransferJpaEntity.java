package br.gravita.adapters.outbound.persistence.entities.inventory;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.inventory.StockTransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "stock_transfers")
public class StockTransferJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "source_warehouse_id", nullable = false)
	private UUID sourceWarehouseId;

	@Column(name = "destination_warehouse_id", nullable = false)
	private UUID destinationWarehouseId;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal quantity;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private StockTransferStatus status;
}
