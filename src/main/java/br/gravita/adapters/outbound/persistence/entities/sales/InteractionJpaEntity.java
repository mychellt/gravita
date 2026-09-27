package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.core.domain.sales.InteractionChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only log row: rows are only ever inserted, never updated, so this does not
 * extend {@code AbstractEntity} (no created_at/modified_at/active bookkeeping needed).
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interactions")
public class InteractionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "opportunity_id")
	private UUID opportunityId;

	@Column(name = "customer_id")
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InteractionChannel channel;

	@Column(nullable = false)
	private String summary;

	@Column(nullable = false)
	private Instant timestamp;
}
