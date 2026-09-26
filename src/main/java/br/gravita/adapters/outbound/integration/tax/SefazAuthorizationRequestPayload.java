package br.gravita.adapters.outbound.integration.tax;

import java.math.BigDecimal;

/**
 * Simplified interop payload for SEFAZ-UF submission - carries only what
 * this ticket's acceptance criteria need (the access key plus the amounts
 * already computed by the shared tax engine). The full modelo 65 XML schema
 * and ICP-Brasil enveloped signature are out of scope here; see the PR notes
 * on GRA-92.
 */
record SefazAuthorizationRequestPayload(String accessKey, BigDecimal saleTotal, BigDecimal taxTotal) {
}
