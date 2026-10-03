package br.gravita.core.domain.mailer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {
    USER_ACTIVATION("user_activation.ftl"),
    CLIENT_CREDENTIAL_ACTIVE("new_access_credential.ftl"),
    CLIENT_CREDENTIAL_INACTIVE("access_credential_inactivated.ftl"),
    CLIENT_CREDENTIAL_REACTIVATED("access_credential_reactivated.ftl"),
    SUPER_USER_ACTIVATION("super_user_activation.ftl"),
    EMAIL_UPDATE_VERIFICATION("user_email_verification.ftl"),
    PASSWORD_RECOVERY("password_recovery.ftl"),
    PASSWORD_CHANGED_BY_ADMIN("password_changed_by_admin.ftl"),
    EMAIL_CHANGED_BY_ADMIN("email_changed_by_admin.ftl");

    private final String templateName;

    private static final Map<String, EmailTemplate> MAP_STRING = new HashMap<>();

    static {
        for (final EmailTemplate type : values()) {
            MAP_STRING.put(type.name().toUpperCase(Locale.ROOT), type);
        }
    }

    public static EmailTemplate entryOf(final String value) {
        return value != null ? MAP_STRING.get(value) : null;
    }

}
