package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.mailer.EmailTemplate;
import br.gravita.core.domain.mailer.Mail;
import br.gravita.core.domain.mailer.MailType;
import br.gravita.core.domain.system.PasswordResetToken;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
import br.gravita.core.ports.outbound.mailer.EmailSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@UseCase
public class SendPasswordResetEmailService implements SendPasswordResetEmailUseCase {

    static final String SUBJECT = "Redefina sua senha Gravita";

    private final EmailSenderPort emailSenderPort;
    private final String linkBaseUrl;
    private final String sender;

    public SendPasswordResetEmailService(final EmailSenderPort emailSenderPort,
                                         @Value("${gravita.password-reset.link-base-url}") final String linkBaseUrl,
                                         @Value("${gravita.mail.sender}") final String sender) {
        this.emailSenderPort = emailSenderPort;
        this.linkBaseUrl = linkBaseUrl;
        this.sender = sender;
    }

    @Override
    public Void execute(final Context context) {
        final var message = context.getData(NotifyPasswordResetMessage.class);
        final var resetUrl = UriComponentsBuilder.fromUriString(linkBaseUrl)
                .queryParam("token", message.token())
                .build().toUriString();

        emailSenderPort.send(Mail.builder()
                .type(MailType.TEMPLATE)
                .template(EmailTemplate.PASSWORD_RESET)
                .from(sender)
                .recipient(message.recipient())
                .subject(SUBJECT)
                .properties(Map.of(
                        "username", message.username(),
                        "resetUrl", resetUrl,
                        "validityMinutes", PasswordResetToken.VALIDITY.toMinutes()))
                .build());
        return null;
    }
}
