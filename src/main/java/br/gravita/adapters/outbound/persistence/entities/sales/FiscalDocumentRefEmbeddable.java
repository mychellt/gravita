package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.core.domain.masterdata.FiscalDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalDocumentRefEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 10)
	private FiscalDocumentType documentType;

	@Column(name = "document_id", nullable = false)
	private UUID documentId;
}
