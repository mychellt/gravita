package br.gravita.core.usercases.system;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.mailer.EmailTemplate;
import br.gravita.core.domain.mailer.Mail;
import br.gravita.core.domain.mailer.MailType;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.ports.outbound.mailer.EmailSenderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendUserRegistrationEmailServiceTest {

    @Mock
    private EmailSenderPort emailSenderPort;

    @Test
    @DisplayName("Sends the activation template to the new user with a link carrying the token")
    void shouldSendTheActivationEmail() {
        final var service = new SendUserRegistrationEmailService(emailSenderPort,
                "http://localhost:8080/activate.html", "gravita@localhost");
        final var message = NotifyUserRegistrationMessage.builder()
                .username("Ana Souza").recipient("ana@acme.com").token("tok_123-abc").tenantId(UUID.randomUUID())
                .build();

        service.execute(new Context(message));

        final var mail = ArgumentCaptor.forClass(Mail.class);
        verify(emailSenderPort).send(mail.capture());
        assertThat(mail.getValue().getType()).isEqualTo(MailType.TEMPLATE);
        assertThat(mail.getValue().getTemplate()).isEqualTo(EmailTemplate.USER_ACTIVATION);
        assertThat(mail.getValue().getFrom()).isEqualTo("gravita@localhost");
        assertThat(mail.getValue().getRecipient()).isEqualTo("ana@acme.com");
        assertThat(mail.getValue().getSubject()).isEqualTo("Ative sua conta Gravita");
        assertThat(mail.getValue().getProperties())
                .containsEntry("username", "Ana Souza")
                .containsEntry("activationUrl", "http://localhost:8080/activate.html?token=tok_123-abc")
                .containsEntry("validityHours", 24L);
    }
}
