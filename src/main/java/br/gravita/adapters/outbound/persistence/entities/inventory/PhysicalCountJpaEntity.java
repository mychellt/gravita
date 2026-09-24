package br.gravita.adapters.outbound.persistence.entities.inventory;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "physical_counts")
public class PhysicalCountJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PhysicalCountScope scope;

	@Column(name = "product_group_id")
	private String productGroupId;

	@Column(name = "warehouse_id", nullable = false)
	private UUID warehouseId;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PhysicalCountStatus status;

	@Column(name = "started_by", nullable = false)
	private UUID startedBy;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@ElementCollection
	@CollectionTable(name = "physical_count_lines", joinColumns = @JoinColumn(name = "physical_count_id"))
	private List<PhysicalCountLineEmbeddable> lines;
}
