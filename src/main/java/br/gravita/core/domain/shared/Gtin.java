package br.gravita.core.domain.shared;

import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor
public final class Gtin {

    private static final Pattern FORMAT = Pattern.compile("\\d{8}|\\d{12,14}");
    private static final Pattern ALL_ZEROS = Pattern.compile("0+");

    public static boolean isValid(final String value) {
        return value != null && FORMAT.matcher(value).matches() && !ALL_ZEROS.matcher(value).matches();
    }
}
