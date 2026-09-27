package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.InboundNfeStatus;
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
@Table(name = "inbound_nfes")
public class InboundNfeJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(name = "access_key", nullable = false, unique = true, length = 44)
	private String accessKey;

	@Column(name = "series", nullable = false, length = 3)
	private String series;

	@Column(name = "number", nullable = false, length = 9)
	private String number;

	@Column(name = "supplier_document", nullable = false, length = 14)
	private String supplierDocument;

	@Column(name = "supplier_name", nullable = false)
	private String supplierName;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "products_value", nullable = false)
	private BigDecimal productsValue;

	@Column(name = "freight_value", nullable = false)
	private BigDecimal freightValue;

	@Column(name = "insurance_value", nullable = false)
	private BigDecimal insuranceValue;

	@Column(name = "discount_value", nullable = false)
	private BigDecimal discountValue;

	@Column(name = "other_expenses_value", nullable = false)
	private BigDecimal otherExpensesValue;

	@Column(name = "icms_value", nullable = false)
	private BigDecimal icmsValue;

	@Column(name = "ipi_value", nullable = false)
	private BigDecimal ipiValue;

	@Column(name = "pis_value", nullable = false)
	private BigDecimal pisValue;

	@Column(name = "cofins_value", nullable = false)
	private BigDecimal cofinsValue;

	@Column(name = "total_value", nullable = false)
	private BigDecimal totalValue;

	@Column(name = "xml_storage_ref", nullable = false)
	private String xmlStorageRef;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InboundNfeStatus status;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

	@ElementCollection
	@CollectionTable(name = "inbound_nfe_items", joinColumns = @JoinColumn(name = "inbound_nfe_id"))
	private List<InboundNfeItemEmbeddable> items;

	@ElementCollection
	@CollectionTable(name = "inbound_nfe_conference_items", joinColumns = @JoinColumn(name = "inbound_nfe_id"))
	private List<InboundNfeConferenceItemEmbeddable> conferenceResult;
}
