package br.gravita.adapters.outbound.mailer;

import br.gravita.core.domain.exceptions.MailException;
import br.gravita.core.domain.mailer.Attachment;
import br.gravita.core.domain.mailer.Mail;
import br.gravita.core.domain.mailer.MailProperties;
import br.gravita.core.ports.outbound.mailer.EmailSenderPort;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Component
public class EmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender javaMailSender;
    private final Configuration freemarkerConfiguration;

    @Override
    public void send(final Mail mail) {
        final var mimeMessage = javaMailSender.createMimeMessage();
        try {
            final var helper = new MimeMessageHelper(mimeMessage, mail.isContainsAttachments());
            helper.setFrom(mail.getFrom());
            if (StringUtils.hasText(mail.getReplyTo())) {
                helper.setReplyTo(mail.getReplyTo());
            }
            helper.setTo(mail.getRecipient());
            helper.setSubject(mail.getSubject());
            if (mail.isContainsAttachments()) {
                addAttachments(helper, mail);
            }
            if (mail.isTemplateBased()) {
                helper.setText(renderTemplate(mail), true);
            } else if (mail.isHtml()) {
                helper.setText(mail.getContent(), true);
            } else {
                helper.setText(mail.getContent());
            }
            javaMailSender.send(mimeMessage);
        } catch (MessagingException | IOException | TemplateException e) {
            throw new MailException("Erro ao tentar enviar e-mail.", e);
        }
    }

    @Override
    public void send(final Mail... mails) {
        Arrays.stream(mails).parallel().forEach(this::send);
    }

    @SuppressWarnings("unchecked")
    private void addAttachments(final MimeMessageHelper helper, final Mail mail) throws MessagingException {
        final var attachments = (List<Attachment>) mail.getProperties().get(MailProperties.ATTACHMENTS.name());
        for (final Attachment attachment : attachments) {
            helper.addAttachment(attachment.getName(), new ByteArrayResource(attachment.getFile()),
                    attachment.getType());
        }
    }

    private String renderTemplate(final Mail mail) throws IOException, TemplateException {
        final var template = freemarkerConfiguration.getTemplate(mail.getTemplate().getTemplateName());
        final var output = new StringWriter();
        template.process(mail.getProperties(), output);
        return output.toString();
    }
}
