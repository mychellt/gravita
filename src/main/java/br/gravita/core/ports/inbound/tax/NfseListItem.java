package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfseDocument;

/** One row of the NFSe list: the document plus the name and state of the municipality ISS is due to (null if unknown). */
public record NfseListItem(NfseDocument document, String municipalityName, String municipalityState) {
}
