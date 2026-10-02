package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.CompanyPerson;

public interface CompanyPersonRepositoryPort {
	CompanyPerson save(CompanyPerson company);

	boolean existsByDocument(String documentNumber);
}
