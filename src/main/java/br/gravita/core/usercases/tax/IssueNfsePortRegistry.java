package br.gravita.core.usercases.tax;

import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Indexes {@link IssueNfsePort} beans by the standard they speak, shared by every use case that talks to one. */
final class IssueNfsePortRegistry {

	private IssueNfsePortRegistry() {
	}

	/** At most one adapter may claim a standard; two fail wiring rather than picking one silently. */
	static Map<NfseStandard, IssueNfsePort> byStandard(final List<IssueNfsePort> issuers) {
		final Map<NfseStandard, IssueNfsePort> byStandard = new EnumMap<>(NfseStandard.class);
		for (final IssueNfsePort issuer : issuers) {
			final IssueNfsePort previous = byStandard.put(issuer.standard(), issuer);
			if (previous != null) {
				throw new IllegalStateException("More than one IssueNfsePort for standard " + issuer.standard() + ": "
						+ previous.getClass().getName() + " and " + issuer.getClass().getName());
			}
		}
		return byStandard;
	}
}
