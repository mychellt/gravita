package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.PaymentMethodType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
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
@Table(name = "cash_closing_reports")
public class CashClosingReportJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "session_id", nullable = false, unique = true)
	private UUID sessionId;

	@Column(name = "register_id", nullable = false)
	private UUID registerId;

	@Column(name = "operator_id", nullable = false)
	private UUID operatorId;

	@Column(name = "opening_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal openingAmount;

	@ElementCollection
	@CollectionTable(name = "cash_closing_report_expected_amounts", joinColumns = @JoinColumn(name = "report_id"))
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(name = "payment_method", length = 20)
	@Column(name = "amount", nullable = false, precision = 14, scale = 2)
	private Map<PaymentMethodType, BigDecimal> expectedAmountsByPaymentMethod;

	@ElementCollection
	@CollectionTable(name = "cash_closing_report_counted_amounts", joinColumns = @JoinColumn(name = "report_id"))
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(name = "payment_method", length = 20)
	@Column(name = "amount", nullable = false, precision = 14, scale = 2)
	private Map<PaymentMethodType, BigDecimal> countedAmountsByPaymentMethod;

	@Column(name = "total_sangria_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalSangriaAmount;

	@Column(name = "total_suprimento_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalSuprimentoAmount;

	@Column(name = "sale_count", nullable = false)
	private int saleCount;

	@Column(name = "opened_at", nullable = false)
	private Instant openedAt;

	@Column(name = "closed_at", nullable = false)
	private Instant closedAt;
}
