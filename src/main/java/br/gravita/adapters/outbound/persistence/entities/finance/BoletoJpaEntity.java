package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.BoletoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "boletos")
public class BoletoJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "receivable_id", nullable = false)
	private UUID receivableId;

	@Enumerated(EnumType.STRING)
	@Column(name = "bank_integration", nullable = false, length = 20)
	private BankIntegration bankIntegration;

	@Column(name = "barcode_line", nullable = false, length = 47)
	private String barcodeLine;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BoletoStatus status;
}
