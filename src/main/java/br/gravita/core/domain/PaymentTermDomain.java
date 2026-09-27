package br.gravita.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PaymentTermDomain extends AbstractDomain {
	private String name;
	private List<Integer> installmentIntervalsDays;

	public int getNumberOfInstallments() {
		return installmentIntervalsDays == null ? 0 : installmentIntervalsDays.size();
	}
}
