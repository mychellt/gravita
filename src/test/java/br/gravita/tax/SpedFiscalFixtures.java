package br.gravita.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Accountant;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.ActivityProfile;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.ActivityType;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Finality;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Period;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Taxpayer;
import java.math.BigDecimal;
import java.util.UUID;

/** What the SPED Fiscal (UC-M2-11) tests share, on top of {@link LivrosFiscaisFixtures}' documents. */
public final class SpedFiscalFixtures {

	private SpedFiscalFixtures() {
	}

	/** The received NFe of {@link LivrosFiscaisFixtures#receivedNfe}, its receipt confirmed. */
	public static InboundNfe confirmed(final InboundNfe received) {
		return received.confirm(received.getItems().stream()
				.map(item -> new InboundNfeConferenceItem(UUID.randomUUID(), item.quantity(), item.quantity()))
				.toList());
	}

	public static Taxpayer taxpayer() {
		return new Taxpayer("Empresa Teste Ltda", "3550308", ActivityProfile.A, ActivityType.OTHER, "Teste",
				"01310100", "100", "Bela Vista");
	}

	public static Accountant accountant() {
		return new Accountant("Contador Teste", "529.982.247-25", "SP-123456/O-0", "contador@example.com");
	}

	public static GenerateSpedFiscalCommand command(final CompanyId companyId, final Period period) {
		return new GenerateSpedFiscalCommand(companyId, period, Finality.ORIGINAL, taxpayer(), accountant());
	}

	public static BigDecimal money(final String value) {
		return new BigDecimal(value);
	}
}
