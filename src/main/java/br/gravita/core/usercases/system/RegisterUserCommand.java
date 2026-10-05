package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserId;

import java.util.UUID;

/** {@code callerId} is the authenticated user registering the account; the new user joins the caller's company. */
public record RegisterUserCommand(String name, String email, String rawPassword, UUID profileId, UserId callerId) {
}
