package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import java.util.regex.Pattern;

public record PixKey(String value, PixKeyType type) {

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final Pattern RANDOM_KEY =
			Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	public static PixKey of(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			throw new BusinessRuleException("PIX key cannot be empty");
		}
		String trimmed = rawValue.trim();

		if (RANDOM_KEY.matcher(trimmed).matches()) {
			return new PixKey(trimmed, PixKeyType.RANDOM);
		}
		if (trimmed.contains("@")) {
			if (!EMAIL.matcher(trimmed).matches()) {
				throw new BusinessRuleException("Invalid PIX key (e-mail): " + rawValue);
			}
			return new PixKey(trimmed, PixKeyType.EMAIL);
		}

		String digits = Document.digitsOnly(trimmed);
		if (digits.length() == 11) {
			return new PixKey(Document.cpf(digits).number(), PixKeyType.CPF);
		}
		if (digits.length() == 14) {
			return new PixKey(Document.cnpj(digits).number(), PixKeyType.CNPJ);
		}
		throw new BusinessRuleException("Invalid PIX key format: " + rawValue);
	}
}
