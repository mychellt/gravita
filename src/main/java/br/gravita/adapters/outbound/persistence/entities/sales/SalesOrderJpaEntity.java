package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.sales.SalesOrderStatus;
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
@Table(name = "sales_orders")
public class SalesOrderJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "origin_quote_id", nullable = false)
	private UUID originQuoteId;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SalesOrderStatus status;

	@ElementCollection
	@CollectionTable(name = "sales_order_items", joinColumns = @JoinColumn(name = "sales_order_id"))
	@OrderColumn(name = "line_index")
	private List<SalesOrderItemEmbeddable> items;
}
