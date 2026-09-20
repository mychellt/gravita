package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;

public class UnknownApprovalModuleException extends BusinessRuleException {

	public UnknownApprovalModuleException(String module) {
		super("Unknown approval module '" + module + "'. Valid modules: purchasing, sales, finance.");
	}
}
