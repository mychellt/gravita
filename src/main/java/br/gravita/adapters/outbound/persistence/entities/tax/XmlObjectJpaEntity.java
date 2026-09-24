package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Backing store for {@code XmlObjectStoragePort} (doc §13: "an object-storage
 * adapter (S3-compatible) for XML/DANFE"). Stored in Postgres for now rather
 * than S3 - no bucket/credentials infra is wired into this repo yet - the
 * port is the swap point when that lands.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "xml_objects")
public class XmlObjectJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(name = "content", nullable = false)
	private byte[] content;
}
