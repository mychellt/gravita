package br.gravita.core.ports.outbound.mailer;

import br.gravita.core.domain.mailer.Mail;

public interface EmailSenderPort {
    void send(final Mail mail);
    void send(final Mail... mails);
}
