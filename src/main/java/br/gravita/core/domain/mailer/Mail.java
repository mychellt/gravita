package br.gravita.core.domain.mailer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mail {

    private String from;

    private String replyTo;

    private String recipient;

    private String subject;

    private String content;

    private MailType type;

    private EmailTemplate template;

    private Map<String, Object> properties;

    public boolean isTemplateBased() {
        return (MailType.TEMPLATE.equals(type));
    }

    public boolean isHtml() {
        return (MailType.HTML.equals(type));
    }

    public boolean isText() {
        return (MailType.TEXT.equals(type));
    }

    public boolean isContainsAttachments() {
        return Objects.nonNull(this.properties) && properties.containsKey(MailProperties.ATTACHMENTS.name());
    }
}
