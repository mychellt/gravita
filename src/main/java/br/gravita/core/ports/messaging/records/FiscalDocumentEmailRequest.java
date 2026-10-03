package br.gravita.core.ports.messaging.records;

import br.gravita.core.ports.messaging.EmailNotificationPort;

import java.util.Objects;

/**
 * UC-M2-03 (AC5): the authorized NFe's XML and rendered DANFE, attached to a
 * single e-mail to the recipient. Unlike {@link EmailNotificationPort}'s
 * plain to/subject/body shape, this carries the two binary attachments a
 * fiscal document delivery always needs.
 */
public record FiscalDocumentEmailRequest(String to, String subject, String body, byte[] xmlContent,
                                         String xmlFilename, byte[] danfeContent, String danfeFilename) {

    public FiscalDocumentEmailRequest {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(xmlContent, "xmlContent");
        Objects.requireNonNull(xmlFilename, "xmlFilename");
        Objects.requireNonNull(danfeContent, "danfeContent");
        Objects.requireNonNull(danfeFilename, "danfeFilename");
    }
}
