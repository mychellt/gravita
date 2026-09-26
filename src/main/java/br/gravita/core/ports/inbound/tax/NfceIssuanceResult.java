package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfceSaleStatus;

/**
 * {@code protocol} is only present for the online path (AC1); a contingency
 * sale (AC2) carries {@code null} here until UC-08 syncs it later.
 */
public record NfceIssuanceResult(NfceSaleStatus status, String accessKey, String protocol) {
}
