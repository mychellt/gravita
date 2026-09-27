package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyCommand;
import br.gravita.core.ports.inbound.tax.ManualInboundNfeData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnterInboundNfeManuallyRequest(UUID companyId, String accessKey, ManualData manualData) {

	public record ManualData(
			String accessKey,
			String series,
			String number,
			String supplierCnpj,
			String supplierName,
			Instant issuedAt,
			List<Item> items,
			Totals totals) {
	}

	public record Item(
			String supplierProductCode,
			String description,
			String ncm,
			String cfop,
			String unit,
			BigDecimal quantity,
			BigDecimal unitValue,
			BigDecimal totalValue,
			BigDecimal icmsValue,
			BigDecimal ipiValue,
			BigDecimal pisValue,
			BigDecimal cofinsValue) {
	}

	public record Totals(
			BigDecimal productsValue,
			BigDecimal freightValue,
			BigDecimal insuranceValue,
			BigDecimal discountValue,
			BigDecimal otherExpensesValue,
			BigDecimal icmsValue,
			BigDecimal ipiValue,
			BigDecimal pisValue,
			BigDecimal cofinsValue,
			BigDecimal totalValue) {
	}

	public EnterInboundNfeManuallyCommand toCommand() {
		return new EnterInboundNfeManuallyCommand(CompanyId.of(companyId), accessKey, toManualData());
	}

	private ManualInboundNfeData toManualData() {
		if (manualData == null) {
			return null;
		}
		return new ManualInboundNfeData(manualData.accessKey(), manualData.series(), manualData.number(),
				Document.cnpj(manualData.supplierCnpj()), manualData.supplierName(), manualData.issuedAt(),
				toItems(manualData.items()), toTotals(manualData.totals()));
	}

	private List<InboundNfeItem> toItems(List<Item> items) {
		if (items == null) {
			return List.of();
		}
		return items.stream()
				.map(item -> new InboundNfeItem(item.supplierProductCode(), item.description(), item.ncm(),
						item.cfop(), item.unit(), item.quantity(), item.unitValue(), item.totalValue(),
						item.icmsValue(), item.ipiValue(), item.pisValue(), item.cofinsValue()))
				.toList();
	}

	private InboundNfeTotals toTotals(Totals totals) {
		if (totals == null) {
			return null;
		}
		return new InboundNfeTotals(totals.productsValue(), totals.freightValue(), totals.insuranceValue(),
				totals.discountValue(), totals.otherExpensesValue(), totals.icmsValue(), totals.ipiValue(),
				totals.pisValue(), totals.cofinsValue(), totals.totalValue());
	}
}
