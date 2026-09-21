package br.gravita.core.usercases.system;

/**
 * Backs both REST calls of UC-M10-05: {@code totpCode} is null on the first call
 * (POST /api/auth/login) and required on the second (POST /api/auth/2fa/verify) whenever the
 * matched user has {@code twoFactorEnabled}. {@code ip}/{@code device} are recorded on every
 * resulting {@code AccessLog} entry regardless of outcome.
 */
public record AuthenticateCommand(String email, String rawPassword, String totpCode, String ip, String device) {
}
