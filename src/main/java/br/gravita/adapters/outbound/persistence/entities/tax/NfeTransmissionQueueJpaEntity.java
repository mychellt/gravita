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

/**
 * Backs {@link br.gravita.core.ports.outbound.tax.TransmissionQueuePort}'s
 * NFe side (UC-M2-01). {@code TransmitNfeUseCase} (UC-M2-03, not built yet)
 * will consume and clear rows from this table.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "nfe_transmission_queue")
public class NfeTransmissionQueueJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "nfe_document_id", nullable = false)
	private UUID nfeDocumentId;

	@Column(name = "queued_at", nullable = false)
	private Instant queuedAt;
}
