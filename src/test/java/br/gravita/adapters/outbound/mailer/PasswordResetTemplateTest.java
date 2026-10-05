package br.gravita.adapters.outbound.mailer;

import br.gravita.core.domain.mailer.EmailTemplate;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders the real template: a syntax slip in an .ftl only shows up when the e-mail is sent. */
class PasswordResetTemplateTest {

    @Test
    @DisplayName("The reset e-mail renders the link as button and plain text, the validity, and escapes the name")
    void shouldRenderTheResetEmail() throws Exception {
        final var configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "mails/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        final var out = new StringWriter();

        configuration.getTemplate(EmailTemplate.PASSWORD_RESET.getTemplateName()).process(Map.of(
                "username", "Ana <b>Souza</b>",
                "resetUrl", "https://app.gravita.test/reset-password?token=abc_123&x=1",
                "validityMinutes", 60L), out);

        final var html = out.toString();
        assertThat(html).contains("Redefinir minha senha")
                .contains("Este link expira em 60 minutos")
                .contains("href=\"https://app.gravita.test/reset-password?token=abc_123&amp;x=1\"")
                .contains("Ana &lt;b&gt;Souza&lt;/b&gt;")
                .doesNotContain("Ativar");
        assertThat(html.split("reset-password\\?token=abc_123", -1)).hasSize(4); // two hrefs + the visible text
    }
}
