package br.gravita.adapters.outbound.integration.tax;

import java.math.BigDecimal;

record SefazAuthorizationRequestPayload(String accessKey, BigDecimal saleTotal, BigDecimal taxTotal) {
}
