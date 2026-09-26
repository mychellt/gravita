package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.NfceSaleStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "nfce_sales")
public class NfceSaleJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "session_id", nullable = false)
	private UUID sessionId;

	@Column(name = "total_discount", nullable = false)
	private BigDecimal totalDiscount;

	@Column(name = "change_given", nullable = false)
	private BigDecimal changeGiven;

	@Column(name = "customer_cpf", length = 11)
	private String customerCpf;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NfceSaleStatus status;

	@Column(name = "registered_at", nullable = false)
	private Instant registeredAt;

	@ElementCollection
	@CollectionTable(name = "nfce_sale_items", joinColumns = @JoinColumn(name = "nfce_sale_id"))
	private List<SaleItemEmbeddable> items;

	@ElementCollection
	@CollectionTable(name = "nfce_sale_payments", joinColumns = @JoinColumn(name = "nfce_sale_id"))
	private List<PaymentEmbeddable> payments;
}
