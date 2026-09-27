package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.sales.QuoteStatus;
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
import java.time.LocalDate;
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
@Table(name = "quotes")
public class QuoteJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "salesperson_id", nullable = false)
	private UUID salespersonId;

	@Column(name = "valid_until", nullable = false)
	private LocalDate validUntil;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuoteStatus status;

	@ElementCollection
	@CollectionTable(name = "quote_items", joinColumns = @JoinColumn(name = "quote_id"))
	@OrderColumn(name = "line_index")
	private List<QuoteItemEmbeddable> items;
}
