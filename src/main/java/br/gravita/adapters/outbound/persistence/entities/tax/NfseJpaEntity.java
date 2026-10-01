package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.PlaceOfProvision;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** The NFSe aggregate; an RPS is the row while {@code status = RPS}, so there is no separate RPS table. */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "nfse_documents",
		uniqueConstraints = { @UniqueConstraint(name = "uk_nfse_documents_rps_number",
				columnNames = { "provider_company_id", "rps_series", "rps_number" }),
				@UniqueConstraint(name = "uk_nfse_documents_nfse_number",
						columnNames = { "provider_company_id", "provider_municipality_ibge", "nfse_series",
								"nfse_number" }) })
public class NfseJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NfseStatus status;

	@Column(name = "provider_company_id", nullable = false)
	private UUID providerCompanyId;

	@Column(name = "provider_municipality_ibge", nullable = false, length = 7)
	private String providerMunicipalityIbge;

	@Column(name = "tomador_person_id")
	private UUID tomadorPersonId;

	@Column(name = "tomador_document", nullable = false, length = 14)
	private String tomadorDocument;

	@Enumerated(EnumType.STRING)
	@Column(name = "tomador_person_type", nullable = false, length = 20)
	private PersonType tomadorPersonType;

	@Column(name = "tomador_name", nullable = false)
	private String tomadorName;

	@Column(name = "tomador_municipality_ibge", length = 7)
	private String tomadorMunicipalityIbge;

	@Column(name = "tomador_street")
	private String tomadorStreet;

	@Column(name = "tomador_number", length = 20)
	private String tomadorNumber;

	@Column(name = "tomador_complement")
	private String tomadorComplement;

	@Column(name = "tomador_neighborhood")
	private String tomadorNeighborhood;

	@Column(name = "tomador_zip_code", length = 10)
	private String tomadorZipCode;

	@Column(name = "tomador_state", length = 2)
	private String tomadorState;

	@Column(name = "service_code", nullable = false, length = 5)
	private String serviceCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "place_of_provision", nullable = false, length = 10)
	private PlaceOfProvision placeOfProvision;

	@Column(name = "iss_municipality_ibge", nullable = false, length = 7)
	private String issMunicipalityIbge;

	@Column(name = "service_amount", nullable = false)
	private BigDecimal serviceAmount;

	@Column(name = "iss_rate", nullable = false)
	private BigDecimal issRate;

	@Column(name = "iss_amount", nullable = false)
	private BigDecimal issAmount;

	@Column(name = "iss_rate_override_justification", length = 500)
	private String issRateOverrideJustification;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String discrimination;

	@Column(name = "rps_series", nullable = false, length = 10)
	private String rpsSeries;

	@Column(name = "rps_number", nullable = false)
	private Long rpsNumber;

	@Column(name = "document_created_at", nullable = false)
	private Instant documentCreatedAt;

	/** Assigned by the RPS -> NFSe conversion (M4-03); {@code null} while the row is still an RPS. */
	@Column(name = "nfse_series", length = 10)
	private String nfseSeries;

	@Column(name = "nfse_number")
	private Long nfseNumber;

	@Column(name = "draft_at")
	private Instant draftAt;

	/** Transmission (M4-04): when it was last sent, the municipality's protocol, and the stored XML's reference. */
	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(length = 100)
	private String protocol;

	@Column(name = "authorized_at")
	private Instant authorizedAt;

	@Column(name = "xml_reference", length = 500)
	private String xmlReference;

	@Column(name = "last_rejection_reason", length = 1000)
	private String lastRejectionReason;

	/** Cancellation (M4-05): the mandatory justification and when the municipality confirmed it. */
	@Column(name = "cancellation_justification", length = 1000)
	private String cancellationJustification;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "nfse_withholdings", joinColumns = @JoinColumn(name = "nfse_document_id"))
	@lombok.Builder.Default
	private List<NfseWithholdingEmbeddable> withholdings = new ArrayList<>();
}
