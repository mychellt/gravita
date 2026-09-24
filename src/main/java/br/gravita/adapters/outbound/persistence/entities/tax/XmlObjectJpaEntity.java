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

	// Without an explicit columnDefinition, Hibernate maps byte[] to a fixed
	// VARBINARY(255) on H2 - fine for the migration's unbounded Postgres BYTEA,
	// but too small for a real NFe XML the moment anything forces a flush (only
	// surfaced once a test issued a query against the DB after storing one -
	// see GRA-62). Matches V26__create_inbound_nfe_tables.sql's column type.
	@Column(name = "content", nullable = false, columnDefinition = "bytea")
	private byte[] content;
}
