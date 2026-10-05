package br.gravita.core.usercases.system;

/** Who is behind a session, as shown in the app's chrome: the user's name and e-mail and the name of their profile. */
public record CurrentUser(String name, String email, String profile) {
}
