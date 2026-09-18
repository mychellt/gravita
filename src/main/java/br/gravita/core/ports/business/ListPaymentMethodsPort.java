package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.PaymentMethodDomain;

import java.util.List;

public interface ListPaymentMethodsPort extends Command<List<PaymentMethodDomain>> {
}
