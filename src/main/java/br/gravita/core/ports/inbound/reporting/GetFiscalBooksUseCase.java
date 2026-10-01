package br.gravita.core.ports.inbound.reporting;

public interface GetFiscalBooksUseCase {
	FiscalBooks execute(FiscalBooksQuery query);
}
