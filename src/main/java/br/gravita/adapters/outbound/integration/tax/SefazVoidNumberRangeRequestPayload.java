package br.gravita.adapters.outbound.integration.tax;

record SefazVoidNumberRangeRequestPayload(String series, Long startNumber, Long endNumber, String justification) {
}
