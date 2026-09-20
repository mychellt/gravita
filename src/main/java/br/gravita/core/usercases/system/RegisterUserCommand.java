package br.gravita.core.usercases.system;

import java.util.UUID;

public record RegisterUserCommand(String name, String email, String rawPassword, UUID profileId) {
}
