package br.gravita.adapters.outbound.mailer;

import br.gravita.core.domain.exceptions.MailException;
import br.gravita.core.domain.mailer.Attachment;
import br.gravita.core.domain.mailer.EmailTemplate;
import br.gravita.core.domain.mailer.MailProperties;
import br.gravita.core.domain.mailer.MailType;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderAdapterTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private Template template;

    @InjectMocks
    private EmailSenderAdapter emailSenderAdapter;

    @BeforeEach
    void setUp() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Sends a plain-text e-mail")
    void shouldSendATextEmail() {
        emailSenderAdapter.send(MailFixture.createValid());

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Sends an HTML e-mail")
    void shouldSendAnHtmlEmail() {
        final var mail = MailFixture.createValid();
        mail.setType(MailType.HTML);

        emailSenderAdapter.send(mail);

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Renders the FreeMarker template with the mail properties and sends it with the attachment")
    void shouldSendATemplateEmailWithAttachments() throws IOException, TemplateException {
        final var mail = MailFixture.createValid();
        mail.setType(MailType.TEMPLATE);
        mail.setTemplate(EmailTemplate.USER_ACTIVATION);
        final var properties = new HashMap<String, Object>();
        properties.put("username", "Ana");
        properties.put(MailProperties.ATTACHMENTS.name(), List.of(Attachment.builder()
                .file(new byte[]{1, 2, 3})
                .name("attachment.txt")
                .type("text/plain")
                .build()));
        mail.setProperties(properties);
        when(freemarkerConfiguration.getTemplate(EmailTemplate.USER_ACTIVATION.getTemplateName()))
                .thenReturn(template);
        doAnswer(call -> {
            ((Writer) call.getArgument(1)).write("<p>Olá</p>");
            return null;
        }).when(template).process(eq(properties), any(Writer.class));

        emailSenderAdapter.send(mail);

        verify(template).process(eq(properties), any(Writer.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("A template that cannot be loaded is reported as a MailException and nothing is sent")
    void shouldWrapTemplateFailuresInAMailException() throws IOException {
        final var mail = MailFixture.createValid();
        mail.setType(MailType.TEMPLATE);
        mail.setTemplate(EmailTemplate.USER_ACTIVATION);
        mail.setProperties(new HashMap<>());
        when(freemarkerConfiguration.getTemplate(any())).thenThrow(new IOException("missing"));

        assertThatThrownBy(() -> emailSenderAdapter.send(mail)).isInstanceOf(MailException.class);

        verify(javaMailSender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("A failure of the mail server is propagated")
    void shouldPropagateMailServerFailures() {
        final var mail = MailFixture.createValid();
        doThrow(new IllegalStateException("smtp down")).when(javaMailSender).send(mimeMessage);

        assertThatThrownBy(() -> emailSenderAdapter.send(mail)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Sends several e-mails in one call")
    void shouldSendSeveralEmails() {
        emailSenderAdapter.send(MailFixture.createValid(), MailFixture.createValid());

        verify(javaMailSender, times(2)).send(mimeMessage);
    }
}
