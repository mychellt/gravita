package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfceSaleStatus;

public record NfceIssuanceResult(NfceSaleStatus status, String accessKey, String protocol) {
}
