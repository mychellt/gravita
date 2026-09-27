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

	@Column(name = "content", nullable = false, columnDefinition = "bytea")
	private byte[] content;
}
