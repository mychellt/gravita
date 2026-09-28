package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
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
@Table(name = "renegotiations")
public class RenegotiationJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "renegotiated_at", nullable = false)
	private Instant renegotiatedAt;

	@ElementCollection
	@CollectionTable(name = "renegotiation_original_receivables",
			joinColumns = @JoinColumn(name = "renegotiation_id"))
	@OrderColumn(name = "position")
	@Column(name = "receivable_id", nullable = false)
	private List<UUID> originalReceivableIds;

	@ElementCollection
	@CollectionTable(name = "renegotiation_new_receivables", joinColumns = @JoinColumn(name = "renegotiation_id"))
	@OrderColumn(name = "position")
	@Column(name = "receivable_id", nullable = false)
	private List<UUID> newReceivableIds;
}
