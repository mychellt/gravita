package br.gravita.adapters.outbound.persistence.entities.finance;

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
@Table(name = "internal_cash_boxes")
public class InternalCashBoxJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal balance;
}
