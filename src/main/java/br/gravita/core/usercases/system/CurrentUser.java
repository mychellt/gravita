package br.gravita.core.usercases.system;

import java.util.UUID;

/**
 * Who is behind a session, as shown in the app's chrome: the user's name and e-mail, the name of their profile and the
 * company they belong to (null for a user without one).
 */
public record CurrentUser(String name, String email, String profile, UUID companyId) {
}
