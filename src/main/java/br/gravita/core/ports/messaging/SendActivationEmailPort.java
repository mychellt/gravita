package br.gravita.core.ports.messaging;

import br.gravita.core.ports.messaging.records.ActivationEmailRequest;

/**
 * Delivers the "activate your account" e-mail to a new signup.
 */
public interface SendActivationEmailPort {

    void send(ActivationEmailRequest request);
}
