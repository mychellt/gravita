package br.gravita.core.usercases.system;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.mailer.EmailTemplate;
import br.gravita.core.domain.mailer.Mail;
import br.gravita.core.domain.mailer.MailType;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
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
class SendPasswordResetEmailServiceTest {

    @Mock
    private EmailSenderPort emailSenderPort;

    @Test
    @DisplayName("Sends the reset template to the user with a link carrying the token and a 60 minute validity")
    void shouldSendTheResetEmail() {
        final var service = new SendPasswordResetEmailService(emailSenderPort,
                "http://localhost:4200/reset-password", "gravita@localhost");
        final var message = NotifyPasswordResetMessage.builder()
                .username("Ana Souza").recipient("ana@acme.com").token("tok_123-abc").tenantId(UUID.randomUUID())
                .build();

        service.execute(new Context(message));

        final var mail = ArgumentCaptor.forClass(Mail.class);
        verify(emailSenderPort).send(mail.capture());
        assertThat(mail.getValue().getType()).isEqualTo(MailType.TEMPLATE);
        assertThat(mail.getValue().getTemplate()).isEqualTo(EmailTemplate.PASSWORD_RESET);
        assertThat(mail.getValue().getFrom()).isEqualTo("gravita@localhost");
        assertThat(mail.getValue().getRecipient()).isEqualTo("ana@acme.com");
        assertThat(mail.getValue().getSubject()).isEqualTo("Redefina sua senha Gravita");
        assertThat(mail.getValue().getProperties())
                .containsEntry("username", "Ana Souza")
                .containsEntry("resetUrl", "http://localhost:4200/reset-password?token=tok_123-abc")
                .containsEntry("validityMinutes", 60L);
    }
}
