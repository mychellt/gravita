package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.finance.SettlementMethod;
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
@Table(name = "settlements")
public class SettlementJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "receivable_id", nullable = false)
	private UUID receivableId;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal interest;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal fine;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal discount;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal surcharge;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SettlementMethod method;

	@Column(name = "settled_at", nullable = false)
	private Instant timestamp;
}
