package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.ManifestationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "inbound_manifestations")
public class InboundManifestationJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "access_key", nullable = false, length = 44)
	private String accessKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ManifestationType type;

	@Column(name = "inbound_nfe_id")
	private UUID inboundNfeId;

	@Column(name = "sefaz_protocol", nullable = false)
	private String sefazProtocol;

	@Column(name = "manifested_at", nullable = false)
	private Instant manifestedAt;
}
