package br.gravita.core.usercases.tax;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.ports.messaging.records.FiscalDocumentEmailRequest;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;

import java.util.Optional;

/**
 * Shared by {@link TransmitNfeService} (automatic send on authorization) and
 * {@link ResendNfeEmailService} (manual resend, UC-M2-03 AC5) so the
 * recipient-lookup and message-shape logic isn't duplicated between them.
 */
final class NfeEmailSupport {

    private NfeEmailSupport() {
    }

    /**
     * A recipient with no linked customer record (an ad-hoc, one-off recipient
     * - see {@link NfeRecipient}'s javadoc) has no e-mail address on file.
     */
    static Optional<String> resolveRecipientEmail(final NfeDocument document, final CustomerRepositoryPort customerRepositoryPort) {
        final NfeRecipient recipient = document.getRecipient();
        if (recipient.personRef() == null) {
            return Optional.empty();
        }
        return customerRepositoryPort.get(recipient.personRef().id())
                .map(CustomerDomain::getEmail)
                .filter(email -> email != null && !email.isBlank());
    }

    static FiscalDocumentEmailRequest buildEmailRequest(final NfeDocument document, final String email, final byte[] xml,
                                                        final byte[] danfe) {
        final String subject = "NFe " + document.getAccessKey() + " autorizada";
        final String body = "A NFe " + document.getDocumentSeries() + "/" + document.getDocumentNumber()
                + " foi autorizada pela SEFAZ. Protocolo: " + document.getSefazProtocol() + ".";
        return new FiscalDocumentEmailRequest(email, subject, body, xml, document.getAccessKey() + ".xml", danfe,
                document.getAccessKey() + ".pdf");
    }
}
