package br.gravita.core.domain.tax;

public enum NaturezaOperacao {
    VENDA,
    DEVOLUCAO,
    NOTA_COMPLEMENTAR,
    TRANSFERENCIA,
    REMESSA,
    OUTRA;
	
    public boolean requiresReferencedAccessKey() {
        return this == DEVOLUCAO || this == NOTA_COMPLEMENTAR;
    }
}
