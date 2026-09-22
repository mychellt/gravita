package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.masterdata.CertificateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "digital_certificates")
public class DigitalCertificateJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false, unique = true)
	private UUID companyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CertificateType type;

	@Column(name = "encrypted_pfx_payload", nullable = false)
	private byte[] encryptedPfxPayload;

	@Column(name = "encrypted_password", nullable = false, columnDefinition = "text")
	private String encryptedPassword;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "uploaded_at", nullable = false)
	private Instant uploadedAt;
}
