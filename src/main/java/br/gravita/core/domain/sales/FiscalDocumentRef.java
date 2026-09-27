package br.gravita.core.domain.sales;

import br.gravita.core.domain.masterdata.FiscalDocumentType;
import java.util.Objects;
import java.util.UUID;

/**
 * A reference to a fiscal document actually issued by {@code tax} (an
 * {@code NfeDocument}, {@code NfceSale} or, once M4 exists, an NFSe
 * document) for one nature-group of a {@link SalesOrder}'s items.
 */
public record FiscalDocumentRef(FiscalDocumentType type, UUID documentId) {

	public FiscalDocumentRef {
		Objects.requireNonNull(type, "type is required");
		Objects.requireNonNull(documentId, "documentId is required");
	}
}
