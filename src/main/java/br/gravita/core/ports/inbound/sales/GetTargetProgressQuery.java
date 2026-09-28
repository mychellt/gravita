package br.gravita.core.ports.inbound.sales;

import java.time.YearMonth;
import java.util.UUID;

public record GetTargetProgressQuery(UUID salesperson, YearMonth month) {
}
