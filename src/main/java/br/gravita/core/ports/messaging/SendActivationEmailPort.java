package br.gravita.core.ports.messaging;

/** Delivers the "activate your account" e-mail to a new signup. */
public interface SendActivationEmailPort {

	void send(ActivationEmailRequest request);
}
