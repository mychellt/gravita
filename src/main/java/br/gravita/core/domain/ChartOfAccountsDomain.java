package br.gravita.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ChartOfAccountsDomain extends AbstractDomain {
	private String code;
	private String name;
	private AccountType accountType;
	private UUID parentId;
}
