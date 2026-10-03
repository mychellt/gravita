package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.ports.messaging.ActivationEmailRequest;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.messaging.SendActivationEmailPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ActivationEmailAdapter implements SendActivationEmailPort {

	private final EmailNotificationPort emailNotificationPort;
	private final String linkBaseUrl;

	public ActivationEmailAdapter(EmailNotificationPort emailNotificationPort,
			@Value("${gravita.activation.link-base-url}") String linkBaseUrl) {
		this.emailNotificationPort = emailNotificationPort;
		this.linkBaseUrl = linkBaseUrl;
	}

	@Override
	public void send(ActivationEmailRequest request) {
		String link = UriComponentsBuilder.fromUriString(linkBaseUrl)
				.queryParam("token", request.activationToken())
				.build().toUriString();
		ActivationEmailTemplate.Rendered email = ActivationEmailTemplate.render(request.recipientName(), link,
				ActivationToken.VALIDITY.toHours());
		emailNotificationPort.sendHtml(request.to(), email.subject(), email.html(), email.plainText());
	}
}
