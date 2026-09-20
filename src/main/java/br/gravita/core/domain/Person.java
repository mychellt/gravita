package br.gravita.core.domain;

import br.gravita.core.domain.shared.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract sealed class Person extends AbstractDomain permits IndividualPerson, CompanyPerson {
	private String name;
	private Document document;
}
