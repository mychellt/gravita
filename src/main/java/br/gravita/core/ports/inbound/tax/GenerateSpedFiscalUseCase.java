package br.gravita.core.ports.inbound.tax;

public interface GenerateSpedFiscalUseCase {
	SpedFiscalFile execute(GenerateSpedFiscalCommand command);
}
