package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
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
@Table(name = "sales_returns")
public class SalesReturnJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "sales_order_id", nullable = false)
	private UUID salesOrderId;

	@Column(name = "total", nullable = false)
	private boolean total;

	@Enumerated(EnumType.STRING)
	@Column(name = "return_nfe_document_type", length = 10)
	private FiscalDocumentType returnNfeDocumentType;

	@Column(name = "return_nfe_document_id")
	private UUID returnNfeDocumentId;

	@ElementCollection
	@CollectionTable(name = "sales_return_items", joinColumns = @JoinColumn(name = "sales_return_id"))
	@OrderColumn(name = "line_index")
	private List<SalesReturnItemEmbeddable> items;
}
