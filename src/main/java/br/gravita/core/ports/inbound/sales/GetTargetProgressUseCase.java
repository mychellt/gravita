package br.gravita.core.ports.inbound.sales;

public interface GetTargetProgressUseCase {
	TargetProgressView execute(GetTargetProgressQuery query);
}
