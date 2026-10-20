package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/** One row of the payables list: the title and its supplier's name ({@code null} for a title without a supplier). */
public record PayableListItem(Payable payable, String supplierName) {
}
