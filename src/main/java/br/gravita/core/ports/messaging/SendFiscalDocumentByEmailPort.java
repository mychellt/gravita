package br.gravita.core.ports.messaging;

import br.gravita.core.ports.messaging.records.FiscalDocumentEmailRequest;

/**
 * UC-M2-03 (AC5): delivers a fiscal document's XML+DANFE to its recipient by
 * e-mail - both on automatic send after authorization, and on a later manual
 * resend.
 */
public interface SendFiscalDocumentByEmailPort {

    void send(FiscalDocumentEmailRequest request);
}
