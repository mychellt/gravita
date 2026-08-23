package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.PlanDomain;

import java.util.List;

public interface ListPlansPort extends Command<List<PlanDomain>> {
}
