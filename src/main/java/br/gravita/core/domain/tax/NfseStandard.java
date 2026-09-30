package br.gravita.core.domain.tax;

/**
 * Communication standards a municipality can speak for NFSe. Adding a value here is a registration-time concern only;
 * whether an {@code IssueNfsePort} adapter exists for it is resolved at transmission time.
 */
public enum NfseStandard {
	ABRASF,
	NFSE_NACIONAL,
	ISSNET,
	BETHA
}
