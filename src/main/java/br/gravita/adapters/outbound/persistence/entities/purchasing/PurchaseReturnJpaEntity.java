package br.gravita.adapters.outbound.persistence.entities.purchasing;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
@Table(name = "purchase_returns")
public class PurchaseReturnJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "receipt_id", nullable = false)
	private UUID receiptId;

	@Column(nullable = false)
	private boolean total;

	@Column(name = "return_nfe_ref")
	private String returnNfeRef;

	@ElementCollection
	@CollectionTable(name = "purchase_return_items", joinColumns = @JoinColumn(name = "purchase_return_id"))
	private List<PurchaseReturnItemEmbeddable> items;
}
