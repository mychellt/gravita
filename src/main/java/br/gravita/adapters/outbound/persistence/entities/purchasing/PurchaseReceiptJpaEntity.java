package br.gravita.adapters.outbound.persistence.entities.purchasing;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
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
@Table(name = "purchase_receipts")
public class PurchaseReceiptJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PurchaseReceiptStatus status;

	@ElementCollection
	@CollectionTable(name = "purchase_receipt_items", joinColumns = @JoinColumn(name = "purchase_receipt_id"))
	private List<PurchaseReceiptItemEmbeddable> items;

	@ElementCollection
	@CollectionTable(name = "purchase_receipt_installments", joinColumns = @JoinColumn(name = "purchase_receipt_id"))
	private List<InstallmentTermEmbeddable> installments;
}
