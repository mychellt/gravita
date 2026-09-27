package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
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
@Table(name = "voided_number_ranges")
public class VoidedNumberRangeJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 10)
	private FiscalDocumentType documentType;

	@Column(length = 10, nullable = false)
	private String series;

	@Column(name = "start_number", nullable = false)
	private Long startNumber;

	@Column(name = "end_number", nullable = false)
	private Long endNumber;

	@Column(nullable = false, length = 500)
	private String justification;

	@Column(name = "sefaz_protocol", nullable = false)
	private String sefazProtocol;

	@Column(name = "voided_at", nullable = false)
	private Instant voidedAt;
}
