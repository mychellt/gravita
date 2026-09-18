package br.gravita.system.adapter.out.persistence;

import br.gravita.system.domain.model.IntegrationEnvironment;
import br.gravita.system.domain.model.IntegrationName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "integration_credential")
public class IntegrationCredentialJpaEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "integration_name", nullable = false, length = 40)
	private IntegrationName integrationName;

	@Enumerated(EnumType.STRING)
	@Column(name = "environment", length = 20)
	private IntegrationEnvironment environment;

	@Column(nullable = false, length = 500)
	private String endpoint;

	@Column(name = "encrypted_credential_payload", nullable = false, columnDefinition = "text")
	private String encryptedCredentialPayload;

	@Column(name = "rotated_at", nullable = false)
	private Instant rotatedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private Instant modifiedAt;
}
