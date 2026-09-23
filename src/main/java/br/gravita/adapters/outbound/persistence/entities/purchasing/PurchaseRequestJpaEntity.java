package br.gravita.adapters.outbound.persistence.entities.purchasing;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
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
@Table(name = "purchase_requests")
public class PurchaseRequestJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PurchaseRequestOrigin origin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PurchaseRequestStatus status;

	@Column(name = "requested_by")
	private UUID requestedBy;

	@ElementCollection
	@CollectionTable(name = "purchase_request_items", joinColumns = @JoinColumn(name = "purchase_request_id"))
	private List<PurchaseRequestItemEmbeddable> items;
}
