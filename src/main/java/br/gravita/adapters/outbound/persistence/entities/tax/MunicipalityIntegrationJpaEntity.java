package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.NfseStandard;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
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
@Table(name = "municipality_integrations",
		uniqueConstraints = @UniqueConstraint(name = "uk_municipality_integrations_ibge_code",
				columnNames = "ibge_code"))
public class MunicipalityIntegrationJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "ibge_code", nullable = false, length = 7)
	private String ibgeCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NfseStandard standard;

	@Column(length = 30)
	private String version;

	@Column(name = "webservice_url", length = 500)
	private String webserviceUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "required_certificate_type", nullable = false, length = 10)
	private CertificateType requiredCertificateType;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "municipality_integration_required_fields",
			joinColumns = @JoinColumn(name = "municipality_integration_id"))
	@OrderColumn(name = "field_index")
	@Column(name = "field_name", nullable = false)
	@lombok.Builder.Default
	private List<String> requiredFields = new ArrayList<>();

	@Column(nullable = false)
	private boolean homologated;
}
