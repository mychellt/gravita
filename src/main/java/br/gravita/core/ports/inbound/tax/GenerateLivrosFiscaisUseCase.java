package br.gravita.core.ports.inbound.tax;

public interface GenerateLivrosFiscaisUseCase {
	LivrosFiscaisReport execute(GenerateLivrosFiscaisCommand command);
}
