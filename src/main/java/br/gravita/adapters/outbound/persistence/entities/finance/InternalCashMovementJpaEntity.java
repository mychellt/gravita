package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.finance.CashMovementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
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
@Table(name = "internal_cash_movements")
public class InternalCashMovementJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "cash_box_id", nullable = false)
	private UUID cashBoxId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CashMovementDirection direction;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 500)
	private String justification;

	@Column(name = "occurred_at", nullable = false)
	private Instant timestamp;
}
