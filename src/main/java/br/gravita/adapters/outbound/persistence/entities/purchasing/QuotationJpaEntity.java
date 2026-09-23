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
@Table(name = "quotations")
public class QuotationJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "request_id", nullable = false)
	private UUID requestId;

	@ElementCollection
	@CollectionTable(name = "quotation_items", joinColumns = @JoinColumn(name = "quotation_id"))
	private List<QuotationItemEmbeddable> items;

	@ElementCollection
	@CollectionTable(name = "quotation_suppliers", joinColumns = @JoinColumn(name = "quotation_id"))
	@Column(name = "supplier_id", nullable = false)
	private List<UUID> suppliers;

	@ElementCollection
	@CollectionTable(name = "quotation_response_lines", joinColumns = @JoinColumn(name = "quotation_id"))
	private List<QuotationResponseLineEmbeddable> responseLines;
}
