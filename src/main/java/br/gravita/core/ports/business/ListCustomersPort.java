package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.CustomerDomain;

import java.util.List;

public interface ListCustomersPort extends Command<List<CustomerDomain>> {
}
