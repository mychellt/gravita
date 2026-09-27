package br.gravita.core.ports.inbound.tax;

/**
 * UC-M2-03 (AC5): the manual resend action for an already-{@code AUTHORIZED}
 * NFe's XML+DANFE, available after the automatic post-authorization e-mail.
 */
public interface ResendNfeEmailUseCase {

	void execute(ResendNfeEmailCommand command);
}
