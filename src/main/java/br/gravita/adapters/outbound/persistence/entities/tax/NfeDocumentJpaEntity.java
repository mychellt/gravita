package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.TransportModality;
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
@Table(name = "nfe_documents")
public class NfeDocumentJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "issuer_company_id", nullable = false)
	private UUID issuerCompanyId;

	@Column(name = "origin_sales_order_id")
	private UUID originSalesOrderId;

	@Column(name = "natureza_operacao", nullable = false)
	private String naturezaOperacao;

	@Column(name = "recipient_customer_id")
	private UUID recipientCustomerId;

	@Column(name = "recipient_document", nullable = false, length = 14)
	private String recipientDocument;

	@Enumerated(EnumType.STRING)
	@Column(name = "recipient_person_type", nullable = false, length = 20)
	private PersonType recipientPersonType;

	@Column(name = "recipient_name", nullable = false)
	private String recipientName;

	@Enumerated(EnumType.STRING)
	@Column(name = "recipient_ie_indicator", nullable = false, length = 20)
	private IeIndicator recipientIeIndicator;

	@Column(name = "recipient_ie", length = 14)
	private String recipientIe;

	@Column(name = "recipient_state", nullable = false, length = 2)
	private String recipientState;

	@Column(name = "freight", nullable = false)
	private BigDecimal freight;

	@Column(name = "insurance", nullable = false)
	private BigDecimal insurance;

	@Column(name = "other_expenses", nullable = false)
	private BigDecimal otherExpenses;

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_modality", length = 3)
	private TransportModality transportModality;

	@Column(name = "transport_carrier")
	private String transportCarrier;

	@Column(name = "transport_volume")
	private String transportVolume;

	@Column(name = "transport_gross_weight")
	private BigDecimal transportGrossWeight;

	@Column(name = "transport_net_weight")
	private BigDecimal transportNetWeight;

	@Column(name = "transport_rntrc")
	private String transportRntrc;

	@Column(name = "referenced_access_key", length = 44)
	private String referencedAccessKey;

	@Column(name = "additional_info")
	private String additionalInfo;

	@Column(name = "icms_total", nullable = false)
	private BigDecimal icmsTotal;

	@Column(name = "icms_st_total", nullable = false)
	private BigDecimal icmsStTotal;

	@Column(name = "ipi_total", nullable = false)
	private BigDecimal ipiTotal;

	@Column(name = "pis_total", nullable = false)
	private BigDecimal pisTotal;

	@Column(name = "cofins_total", nullable = false)
	private BigDecimal cofinsTotal;

	@Column(name = "fcp_total", nullable = false)
	private BigDecimal fcpTotal;

	@Column(name = "grand_total", nullable = false)
	private BigDecimal grandTotal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NfeDocumentStatus status;

	@Column(length = 3)
	private String series;

	private Long number;

	@Column(name = "access_key", length = 44, unique = true)
	private String accessKey;

	private String protocol;

	@Column(name = "drafted_at", nullable = false)
	private Instant draftedAt;

	@ElementCollection
	@CollectionTable(name = "nfe_document_items", joinColumns = @JoinColumn(name = "nfe_document_id"))
	private List<NfeDocumentItemEmbeddable> items;
}
