package br.gravita.core.ports.outbound.finance;

public interface NotifyNegativeBalanceProjectionPort {

	void notify(NegativeBalanceProjectionAlert alert);
}
