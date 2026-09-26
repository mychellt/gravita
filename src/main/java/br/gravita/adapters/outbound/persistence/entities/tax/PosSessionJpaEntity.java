package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.PosSessionStatus;
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
@Table(name = "pos_sessions")
public class PosSessionJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "register_id", nullable = false)
	private UUID registerId;

	@Column(name = "operator_id", nullable = false)
	private UUID operatorId;

	@Column(name = "opening_change_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal openingChangeAmount;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PosSessionStatus status;

	@Column(name = "opened_at", nullable = false)
	private Instant openedAt;

	@Column(name = "closed_at")
	private Instant closedAt;
}
