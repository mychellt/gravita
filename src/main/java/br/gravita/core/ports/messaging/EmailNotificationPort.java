package br.gravita.core.ports.messaging;

public interface EmailNotificationPort {
	void send(String to, String subject, String body);
}
