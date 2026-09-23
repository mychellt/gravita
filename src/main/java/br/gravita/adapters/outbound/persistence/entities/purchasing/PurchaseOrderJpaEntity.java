package br.gravita.adapters.outbound.persistence.entities.purchasing;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
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
@Table(name = "purchase_orders")
public class PurchaseOrderJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "request_id", nullable = false)
	private UUID requestId;

	@Column(name = "quotation_id")
	private UUID quotationId;

	@Column(name = "supplier_id", nullable = false)
	private UUID supplierId;

	@Column(name = "approval_required", nullable = false)
	private boolean approvalRequired;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PurchaseOrderStatus status;

	@ElementCollection
	@CollectionTable(name = "purchase_order_items", joinColumns = @JoinColumn(name = "purchase_order_id"))
	private List<PurchaseOrderItemEmbeddable> items;
}
