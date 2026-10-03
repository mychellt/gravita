package br.gravita.core.ports.messaging;

public interface EmailNotificationPort {
	void send(String to, String subject, String body);

	/** Sends an HTML e-mail; {@code plainTextBody} is the fallback part for clients that do not render HTML. */
	void sendHtml(String to, String subject, String htmlBody, String plainTextBody);
}
