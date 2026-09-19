package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.FiscalDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_series")
public class DocumentSeriesJpaEntity implements Persistable<UUID> {

	@Id
	private UUID id;

	// id is always application-assigned (DocumentSeries.placeholder); the
	// repository adapter sets this from existsById before save so Spring Data
	// picks persist() over merge() for a row that doesn't exist yet.
	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 10)
	private FiscalDocumentType documentType;

	@Column(length = 10)
	private String series;

	@Column(name = "next_number")
	private Long nextNumber;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private LocalDateTime modifiedAt;
}
