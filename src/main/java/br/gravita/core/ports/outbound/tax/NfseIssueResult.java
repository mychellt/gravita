package br.gravita.core.ports.outbound.tax;

import java.time.Instant;
import java.util.Objects;

/**
 * What a municipality answered to an issuance request: either an authorization (with the signed XML that was
 * transmitted, so the use case can store it) or an active rejection. Build it with {@link #authorized} or
 * {@link #rejected}.
 */
public record NfseIssueResult(String protocol, Instant authorizedAt, byte[] xml, String rejectionReason) {

	public NfseIssueResult {
		final boolean authorized = protocol != null;
		if (authorized == (rejectionReason != null)) {
			throw new IllegalArgumentException("exactly one of protocol or rejectionReason must be present");
		}
		if (authorized) {
			Objects.requireNonNull(authorizedAt, "authorizedAt");
			Objects.requireNonNull(xml, "xml");
		}
	}

	public static NfseIssueResult authorized(final String protocol, final Instant authorizedAt, final byte[] xml) {
		return new NfseIssueResult(protocol, authorizedAt, xml, null);
	}

	public static NfseIssueResult rejected(final String reason) {
		return new NfseIssueResult(null, null, null, reason);
	}

	public boolean isAuthorized() {
		return protocol != null;
	}
}
