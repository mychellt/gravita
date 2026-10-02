package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;

/** A self-service signup was refused; {@code field} names the form field the message is about. */
public class SignupRejectedException extends BusinessRuleException {

	public static final String EMAIL = "email";
	public static final String CNPJ = "cnpj";
	public static final String PLAN = "plan";
	public static final String BILLING = "billing";

	private final String field;

	public SignupRejectedException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
