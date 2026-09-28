package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "payables")
public class PayableJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "supplier_id")
	private UUID supplierId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PayableOrigin origin;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@ElementCollection
	@CollectionTable(name = "payable_cost_center_splits", joinColumns = @JoinColumn(name = "payable_id"))
	@OrderColumn(name = "position")
	private List<CostCenterShareEmbeddable> costCenterSplit;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PayableStatus status;
}
