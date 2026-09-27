package br.gravita.core.ports.inbound.sales;

public interface GetFunnelConversionUseCase {
	FunnelConversionView execute(GetFunnelConversionQuery query);
}
