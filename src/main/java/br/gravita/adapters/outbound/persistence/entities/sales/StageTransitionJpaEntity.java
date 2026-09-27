package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.core.domain.sales.OpportunityStage;
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
@Table(name = "opportunity_stage_transitions")
public class StageTransitionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "opportunity_id", nullable = false)
	private UUID opportunityId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_stage", nullable = false, length = 20)
	private OpportunityStage fromStage;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_stage", nullable = false, length = 20)
	private OpportunityStage toStage;

	@Column(nullable = false)
	private Instant timestamp;
}
