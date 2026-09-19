package br.gravita.system.application.port.in;

import java.util.UUID;

public record RegisterUserCommand(String name, String email, String rawPassword, UUID profileId) {
}
