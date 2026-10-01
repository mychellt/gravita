package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.NfseMunicipalityUnavailableException;
import br.gravita.core.domain.tax.NfseStandard;

/**
 * Transmits an NFSe to a municipality over one {@link NfseStandard}'s webservice. There is one implementation per
 * standard (ABRASF, NFS-e Nacional, ISS.net, Betha); {@code TransmitNfseService} picks the one whose
 * {@link #standard()} matches the municipality's registered standard, so supporting a new standard means adding an
 * implementation - nothing in the use case changes.
 *
 * <p>Signing the XML with the provider's certificate happens inside the implementation, never in the use case.
 */
public interface IssueNfsePort {

	/** The standard this adapter speaks; exactly one adapter may claim each standard. */
	NfseStandard standard();

	/**
	 * Builds, signs and submits the NFSe.
	 *
	 * @throws NfseMunicipalityUnavailableException when the municipality cannot be reached or does not answer in time
	 */
	NfseIssueResult issue(NfseIssueRequest request);
}
