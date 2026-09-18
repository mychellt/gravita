package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.PaymentTermDomain;

import java.util.List;

public interface ListPaymentTermsPort extends Command<List<PaymentTermDomain>> {
}
