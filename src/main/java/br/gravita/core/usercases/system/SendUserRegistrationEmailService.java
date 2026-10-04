package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.mailer.EmailTemplate;
import br.gravita.core.domain.mailer.Mail;
import br.gravita.core.domain.mailer.MailType;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.ports.outbound.mailer.EmailSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@UseCase
public class SendUserRegistrationEmailService implements SendUserRegistrationEmailUseCase {

    static final String SUBJECT = "Ative sua conta Gravita";

    private final EmailSenderPort emailSenderPort;
    private final String linkBaseUrl;
    private final String sender;

    public SendUserRegistrationEmailService(final EmailSenderPort emailSenderPort,
                                            @Value("${gravita.activation.link-base-url}") final String linkBaseUrl,
                                            @Value("${gravita.mail.sender}") final String sender) {
        this.emailSenderPort = emailSenderPort;
        this.linkBaseUrl = linkBaseUrl;
        this.sender = sender;
    }

    @Override
    public Void execute(final Context context) {
        final var message = context.getData(NotifyUserRegistrationMessage.class);
        final var activationUrl = UriComponentsBuilder.fromUriString(linkBaseUrl)
                .queryParam("token", message.token())
                .build().toUriString();

        emailSenderPort.send(Mail.builder()
                .type(MailType.TEMPLATE)
                .template(EmailTemplate.USER_ACTIVATION)
                .from(sender)
                .recipient(message.recipient())
                .subject(SUBJECT)
                .properties(Map.of(
                        "username", message.username(),
                        "activationUrl", activationUrl,
                        "validityHours", ActivationToken.VALIDITY.toHours()))
                .build());
        return null;
    }
}
