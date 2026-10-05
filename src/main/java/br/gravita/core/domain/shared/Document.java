package br.gravita.core.domain.shared;

import java.util.regex.Pattern;

public record Document(String number, PersonType personType) {

	private static final Pattern NON_DIGIT = Pattern.compile("\\D");

	public Document {
		if (number == null || number.isBlank()) {
			throw new BusinessRuleException("Document cannot be empty");
		}
	}

	public static Document cpf(final String value) {
		final String digits = digitsOnly(value);
		if (!isValidCpf(digits)) {
			throw new BusinessRuleException("Invalid CPF: " + value);
		}
		return new Document(digits, PersonType.INDIVIDUAL);
	}

	public static Document cnpj(final String value) {
		final String digits = digitsOnly(value);
		if (!isValidCnpj(digits)) {
			throw new BusinessRuleException("Invalid CNPJ: " + value);
		}
		return new Document(digits, PersonType.COMPANY);
	}

	public String formatted() {
		return personType == PersonType.INDIVIDUAL ? formatCpf() : formatCnpj();
	}

	public static String digitsOnly(final String value) {
		return NON_DIGIT.matcher(value == null ? "" : value).replaceAll("");
	}

	private String formatCpf() {
		return number.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
	}

	private String formatCnpj() {
		return number.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
	}

	private static boolean isValidCpf(final String cpf) {
		if (cpf.length() != 11 || allDigitsEqual(cpf)) {
			return false;
		}
		final int dv1 = calculateCheckDigit(cpf.substring(0, 9), 10);
		final int dv2 = calculateCheckDigit(cpf.substring(0, 9) + dv1, 11);
		return cpf.equals(cpf.substring(0, 9) + dv1 + dv2);
	}

	private static boolean isValidCnpj(final String cnpj) {
		if (cnpj.length() != 14 || allDigitsEqual(cnpj)) {
			return false;
		}
		final int[] weightsDv1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		final int[] weightsDv2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		final int dv1 = calculateCheckDigit(cnpj.substring(0, 12), weightsDv1);
		final int dv2 = calculateCheckDigit(cnpj.substring(0, 12) + dv1, weightsDv2);
		return cnpj.equals(cnpj.substring(0, 12) + dv1 + dv2);
	}

	private static boolean allDigitsEqual(final String value) {
		return value.chars().distinct().count() == 1;
	}

	private static int calculateCheckDigit(final String base, final int startingWeight) {
		int sum = 0;
		int weight = startingWeight;
		for (int i = 0; i < base.length(); i++) {
			sum += Character.getNumericValue(base.charAt(i)) * weight--;
		}
		final int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}

	private static int calculateCheckDigit(final String base, final int[] weights) {
		int sum = 0;
		for (int i = 0; i < base.length(); i++) {
			sum += Character.getNumericValue(base.charAt(i)) * weights[i];
		}
		final int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}
}
