package br.gravita.core.ports.inbound.sales;

import java.time.YearMonth;
import java.util.UUID;

public record GetFunnelConversionQuery(YearMonth period, UUID salesperson) {
}
