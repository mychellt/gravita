package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
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
public class NfeJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "issuer_company_id", nullable = false)
	private UUID issuerCompanyId;

	@Column(name = "origin_sales_order_id")
	private UUID originSalesOrderId;

	@Column(name = "natureza_operacao", nullable = false, length = 30)
	private String naturezaOperacao;

	@Column(name = "cfop", nullable = false, length = 4)
	private String cfop;

	@Column(name = "recipient_person_id")
	private UUID recipientPersonId;

	@Column(name = "recipient_document", nullable = false, length = 14)
	private String recipientDocument;

	@Enumerated(EnumType.STRING)
	@Column(name = "recipient_person_type", nullable = false, length = 20)
	private PersonType recipientPersonType;

	@Column(name = "recipient_name", nullable = false)
	private String recipientName;

	@Column(name = "recipient_state_registration", length = 20)
	private String recipientStateRegistration;

	@Column(name = "recipient_state", nullable = false, length = 2)
	private String recipientState;

	@Column(name = "freight", nullable = false)
	private BigDecimal freight;

	@Column(name = "insurance", nullable = false)
	private BigDecimal insurance;

	@Column(name = "other_expenses", nullable = false)
	private BigDecimal otherExpenses;

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_modality", length = 10)
	private TransportModality transportModality;

	@Column(name = "transport_carrier")
	private String transportCarrier;

	@Column(name = "transport_volume")
	private Integer transportVolume;

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NfeDocumentStatus status;

	@Column(name = "document_created_at", nullable = false)
	private Instant documentCreatedAt;

	@Column(name = "document_series", length = 3)
	private String documentSeries;

	@Column(name = "document_number")
	private Long documentNumber;

	@Column(name = "access_key", length = 44)
	private String accessKey;

	@Column(name = "sefaz_protocol")
	private String sefazProtocol;

	@Column(name = "contingency_mode", nullable = false)
	private boolean contingencyMode;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "xml_storage_ref")
	private String xmlStorageRef;

	@Column(name = "danfe_storage_ref")
	private String danfeStorageRef;

	@ElementCollection
	@CollectionTable(name = "nfe_items", joinColumns = @JoinColumn(name = "nfe_document_id"))
	private List<NfeItemEmbeddable> items;

	@ElementCollection
	@CollectionTable(name = "nfe_item_tax_lines", joinColumns = @JoinColumn(name = "nfe_document_id"))
	private List<NfeItemTaxLineEmbeddable> taxLines;
}
