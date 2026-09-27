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

	@Column(name = "attempts", nullable = false)
	private int attempts;

	@Column(name = "next_retry_at", nullable = false)
	private Instant nextRetryAt;
}
