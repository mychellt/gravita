package br.gravita.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethodType {
	CASH("Dinheiro"),
	DEBIT_CARD("Cartão de débito"),
	CREDIT_CARD("Cartão de crédito"),
	PIX("PIX"),
	BOLETO("Boleto bancário"),
	STORE_CREDIT("Crédito na loja"),
	VOUCHER("Vale");

	private final String description;
}
