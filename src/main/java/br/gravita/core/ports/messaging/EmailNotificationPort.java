package br.gravita.core.ports.messaging;

public interface EmailNotificationPort {
    void send(String to, String subject, String body);

    void sendHtml(String to, String subject, String htmlBody, String plainTextBody);
}
