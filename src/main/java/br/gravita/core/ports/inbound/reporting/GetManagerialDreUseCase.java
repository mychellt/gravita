package br.gravita.core.ports.inbound.reporting;

public interface GetManagerialDreUseCase {
	ManagerialDre execute(DreQuery query);
}
