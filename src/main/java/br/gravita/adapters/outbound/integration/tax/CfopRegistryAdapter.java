package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.ports.outbound.tax.CfopRegistryPort;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Default CFOP registry (UC-M2-01, AC2): a fixed table of the most common
 * intrastate operation CFOPs, keyed by {@link NaturezaOperacao}. This is a
 * placeholder until the "free CFOP registration" screen from doc §3.1 exists
 * as a proper M1-style auxiliary table with admin CRUD and interstate-CFOP
 * variants; tracked as a follow-up rather than built here.
 */
@Component
public class CfopRegistryAdapter implements CfopRegistryPort {

	private static final Map<NaturezaOperacao, Cfop> DEFAULT_CFOPS = Map.of(
			NaturezaOperacao.VENDA, new Cfop("5102"),
			NaturezaOperacao.DEVOLUCAO, new Cfop("5202"),
			NaturezaOperacao.NOTA_COMPLEMENTAR, new Cfop("5124"),
			NaturezaOperacao.TRANSFERENCIA, new Cfop("5152"),
			NaturezaOperacao.REMESSA, new Cfop("5915"),
			NaturezaOperacao.OUTRA, new Cfop("5949"));

	@Override
	public Cfop resolve(final NaturezaOperacao naturezaOperacao) {
		final Cfop cfop = DEFAULT_CFOPS.get(naturezaOperacao);
		if (cfop == null) {
			throw new BusinessRuleException("No CFOP registered for operation type: " + naturezaOperacao);
		}
		return cfop;
	}
}
