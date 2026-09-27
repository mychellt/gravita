package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.util.regex.Pattern;

/**
 * A 4-digit CFOP (Código Fiscal de Operações e Prestações) code. Always
 * produced by {@link br.gravita.core.ports.outbound.tax.CfopRegistryPort},
 * never constructed from a caller-supplied literal in the issuance flow
 * (UC-M2-01, AC2) — the registry is the single place a CFOP is looked up.
 */
public record Cfop(String code) {

	private static final Pattern CFOP_CODE = Pattern.compile("\\d{4}");

	public Cfop {
		if (code == null || !CFOP_CODE.matcher(code).matches()) {
			throw new BusinessRuleException("Invalid CFOP code: " + code);
		}
	}
}
