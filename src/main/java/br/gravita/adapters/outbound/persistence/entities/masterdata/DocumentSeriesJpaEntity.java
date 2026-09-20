package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_series")
public class DocumentSeriesJpaEntity extends AbstractEntity<UUID> {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 10)
	private FiscalDocumentType documentType;

	@Column(length = 10)
	private String series;

	@Column(name = "next_number")
	private Long nextNumber;
}
