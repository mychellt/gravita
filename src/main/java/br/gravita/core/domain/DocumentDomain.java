package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;

import java.util.regex.Pattern;

public record DocumentDomain(String number, PersonType personType) {

	private static final Pattern NON_DIGIT = Pattern.compile("\\D");

	public DocumentDomain {
		if (number == null || number.isBlank()) {
			throw new BusinessRuleException("Document cannot be empty");
		}
	}

	public static DocumentDomain cpf(String value) {
		String digits = digitsOnly(value);
		if (!isValidCpf(digits)) {
			throw new BusinessRuleException("Invalid CPF: " + value);
		}
		return new DocumentDomain(digits, PersonType.INDIVIDUAL);
	}

	public static DocumentDomain cnpj(String value) {
		String digits = digitsOnly(value);
		if (!isValidCnpj(digits)) {
			throw new BusinessRuleException("Invalid CNPJ: " + value);
		}
		return new DocumentDomain(digits, PersonType.COMPANY);
	}

	public String formatted() {
		return personType == PersonType.INDIVIDUAL ? formatCpf() : formatCnpj();
	}

	private String formatCpf() {
		return number.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
	}

	private String formatCnpj() {
		return number.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
	}

	private static String digitsOnly(String value) {
		return NON_DIGIT.matcher(value == null ? "" : value).replaceAll("");
	}

	private static boolean isValidCpf(String cpf) {
		if (cpf.length() != 11 || allDigitsEqual(cpf)) {
			return false;
		}
		int dv1 = calculateCheckDigit(cpf.substring(0, 9), 10);
		int dv2 = calculateCheckDigit(cpf.substring(0, 9) + dv1, 11);
		return cpf.equals(cpf.substring(0, 9) + dv1 + dv2);
	}

	private static boolean isValidCnpj(String cnpj) {
		if (cnpj.length() != 14 || allDigitsEqual(cnpj)) {
			return false;
		}
		int[] weightsDv1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		int[] weightsDv2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		int dv1 = calculateCheckDigit(cnpj.substring(0, 12), weightsDv1);
		int dv2 = calculateCheckDigit(cnpj.substring(0, 12) + dv1, weightsDv2);
		return cnpj.equals(cnpj.substring(0, 12) + dv1 + dv2);
	}

	private static boolean allDigitsEqual(String value) {
		return value.chars().distinct().count() == 1;
	}

	private static int calculateCheckDigit(String base, int startingWeight) {
		int sum = 0;
		int weight = startingWeight;
		for (int i = 0; i < base.length(); i++) {
			sum += Character.getNumericValue(base.charAt(i)) * weight--;
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}

	private static int calculateCheckDigit(String base, int[] weights) {
		int sum = 0;
		for (int i = 0; i < base.length(); i++) {
			sum += Character.getNumericValue(base.charAt(i)) * weights[i];
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}
}
