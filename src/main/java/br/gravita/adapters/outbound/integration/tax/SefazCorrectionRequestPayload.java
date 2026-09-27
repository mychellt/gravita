package br.gravita.adapters.outbound.integration.tax;

record SefazCorrectionRequestPayload(String accessKey, int sequenceNumber, String text) {
}
