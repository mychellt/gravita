package br.gravita.adapters.outbound.integration.tax;

/**
 * Simplified interop payload for a SEFAZ-UF `Inutilização` event - see
 * {@link SefazAuthorizationRequestPayload}'s note on scope for the same
 * simplification applied here.
 */
record SefazVoidRangeRequestPayload(String series, Long startNumber, Long endNumber, String justification) {
}
