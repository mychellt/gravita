package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

<<<<<<< HEAD
/**
 * Backs {@link br.gravita.core.ports.outbound.tax.TransmissionQueuePort}, now
 * shared across M2 (NFe, UC-M2-01 AC7) and M3 (NFC-e contingency, AC2)
 * despite the table's NFC-e-origin name. {@code SyncContingencySalesUseCase}
 * (NFC-e) and {@code TransmitNfeUseCase} (NFe, GRA-103) will consume and
 * clear rows from this table; neither is built yet.
 */
=======
>>>>>>> origin/master
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "nfce_contingency_queue")
public class NfceContingencyQueueJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "nfce_sale_id", nullable = false)
	private UUID documentId;

	@Column(name = "queued_at", nullable = false)
	private Instant queuedAt;
}
