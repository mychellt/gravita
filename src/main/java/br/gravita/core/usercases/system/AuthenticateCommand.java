package br.gravita.core.usercases.system;

public record AuthenticateCommand(String email, String rawPassword, String totpCode, String ip, String device) {
}
