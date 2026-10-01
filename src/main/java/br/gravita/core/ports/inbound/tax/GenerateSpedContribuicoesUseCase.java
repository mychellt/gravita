package br.gravita.core.ports.inbound.tax;

public interface GenerateSpedContribuicoesUseCase {
	SpedContribuicoesFile execute(GenerateSpedContribuicoesCommand command);
}
