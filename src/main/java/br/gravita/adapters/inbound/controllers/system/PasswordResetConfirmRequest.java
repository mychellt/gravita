package br.gravita.adapters.inbound.controllers.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The token is not validated here: a missing one must get the same "invalid link" answer as an unknown one. */
public record PasswordResetConfirmRequest(String token, @NotBlank @Size(max = 128) String newPassword) {
}
