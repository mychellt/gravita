package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.ports.inbound.tax.NfseTransmissionResult;

/**
 * Non-homologated fallback: renders the document as the XML its municipality's standard expects, plus the steps to
 * upload it on the municipality's own portal. Must work for every {@code NfseStandard} - it is what keeps the
 * operation from ever being fully blocked.
 */
public interface GenerateGuidedManualUploadPort {

	NfseTransmissionResult.GuidedManualUpload generate(NfseDocument document, MunicipalityIntegration integration);
}
