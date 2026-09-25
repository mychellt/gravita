package br.gravita.adapters.outbound.persistence.entities.inventory;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.inventory.StockMovementType;
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
import java.time.Instant;
import java.util.List;
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
@Table(name = "stock_movements")
public class StockMovementJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private StockMovementType type;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "warehouse_id", nullable = false)
	private UUID warehouseId;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal quantity;

	@Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
	private BigDecimal unitCost;

	@Column(name = "lot_code")
	private String lotCode;

	@ElementCollection
	@CollectionTable(name = "stock_movement_serial_numbers", joinColumns = @JoinColumn(name = "stock_movement_id"))
	@Column(name = "serial_number", nullable = false)
	private List<String> serialNumbers;

	@Column(name = "origin_reference", nullable = false)
	private String originReference;

	@Column(name = "justification")
	private String justification;

	@Column(name = "user_id", nullable = false)
	private UUID user;

	@Column(name = "movement_at", nullable = false)
	private Instant timestamp;
}
