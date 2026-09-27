package br.gravita.adapters.outbound.integration.tax;

/**
 * Simplified interop payload for a SEFAZ-UF cancellation event - see
 * {@link SefazAuthorizationRequestPayload}'s note on scope for the same
 * simplification applied here.
 */
record SefazCancellationRequestPayload(String accessKey, String protocol, String reason) {
}
