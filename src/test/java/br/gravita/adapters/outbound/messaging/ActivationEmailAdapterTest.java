package br.gravita.adapters.outbound.messaging;

import br.gravita.core.ports.messaging.ActivationEmailRequest;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivationEmailAdapterTest {

	private static final String TOKEN = "abc123-_TOKEN";
	private static final String EXPECTED_LINK = "https://app.gravita.test/activate?token=" + TOKEN;

	@Mock
	private EmailNotificationPort emailNotificationPort;

	private ActivationEmailAdapter adapter() {
		return new ActivationEmailAdapter(emailNotificationPort, "https://app.gravita.test/activate");
	}

	private ActivationEmailRequest request(String name) {
		return new ActivationEmailRequest("ana@acme.com", name, TOKEN, Instant.parse("2026-01-11T12:00:00Z"));
	}

	private String[] sentEmail() {
		ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(emailNotificationPort).sendHtml(to.capture(), subject.capture(), html.capture(), text.capture());
		return new String[] {to.getValue(), subject.getValue(), html.getValue(), text.getValue()};
	}

	private static int occurrences(String text, String fragment) {
		return text.split(java.util.regex.Pattern.quote(fragment), -1).length - 1;
	}

	@Test
	@DisplayName("The e-mail is HTML addressed to the signup, with a plain-text alternative")
	void shouldSendHtmlWithAPlainTextPart() {
		adapter().send(request("Ana Souza"));

		String[] email = sentEmail();
		assertThat(email[0]).isEqualTo("ana@acme.com");
		assertThat(email[1]).isEqualTo("Ative sua conta Gravita");
		assertThat(email[2]).contains("<html").contains("</html>").contains("<a href=");
		assertThat(email[3]).isNotBlank().doesNotContain("<");
	}

	@Test
	@DisplayName("The greeting uses the signup's name in both parts")
	void shouldGreetTheSignupByName() {
		adapter().send(request("Ana Souza"));

		String[] email = sentEmail();
		assertThat(email[2]).contains("Olá, Ana Souza!");
		assertThat(email[3]).contains("Olá, Ana Souza!");
	}

	@Test
	@DisplayName("There is exactly one link and it carries the token")
	void shouldHaveExactlyOneTokenLink() {
		adapter().send(request("Ana Souza"));

		String[] email = sentEmail();
		assertThat(occurrences(email[2], "<a ")).isEqualTo(1);
		assertThat(occurrences(email[2], TOKEN)).isEqualTo(1);
		assertThat(email[2]).contains("href=\"" + EXPECTED_LINK + "\"");
		assertThat(occurrences(email[3], TOKEN)).isEqualTo(1);
		assertThat(email[3]).contains(EXPECTED_LINK);
	}

	@Test
	@DisplayName("It states explicitly that the link expires in 24 hours, in both parts")
	void shouldStateTheTwentyFourHourExpiry() {
		adapter().send(request("Ana Souza"));

		String[] email = sentEmail();
		assertThat(email[2]).contains("Este link expira em 24 horas.");
		assertThat(email[3]).contains("Este link expira em 24 horas.");
	}

	@Test
	@DisplayName("It carries the Gravita branding with inline CSS")
	void shouldCarryBrandingWithInlineCss() {
		adapter().send(request("Ana Souza"));

		String html = sentEmail()[2];
		assertThat(html).contains("Gravita").contains("background-color:#4F6EF7").doesNotContain("<style");
	}

	@Test
	@DisplayName("A name containing markup cannot inject HTML into the e-mail")
	void shouldEscapeTheName() {
		adapter().send(request("<script>alert(1)</script>"));

		String[] email = sentEmail();
		assertThat(email[2]).doesNotContain("<script>").contains("&lt;script&gt;");
	}
}
