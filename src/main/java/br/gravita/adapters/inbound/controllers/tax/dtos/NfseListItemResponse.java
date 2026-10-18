package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.ports.inbound.tax.NfseListItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code number} is the NFSe number once converted, {@code rpsNumber} always; {@code issuedAt} is the authorization time, else creation. */
public record NfseListItemResponse(UUID id, NfseStatus status, String series, Long number, String rpsSeries, Long rpsNumber,
		String tomadorName, String tomadorDocument, String municipalityIbgeCode, String municipalityName,
		String municipalityState, String serviceCode, String discrimination, BigDecimal serviceAmount, BigDecimal issRate,
		BigDecimal issAmount, BigDecimal withheldAmount, Instant issuedAt) {

	public static NfseListItemResponse from(final NfseListItem item) {
		final NfseDocument d = item.document();
		return new NfseListItemResponse(d.getId().value(), d.getStatus(), d.getNfseSeries(), d.getNfseNumber(),
				d.getRpsSeries(), d.getRpsNumber(), d.getTomador().name(), d.getTomador().document().number(),
				d.getIssMunicipalityIbgeCode(), item.municipalityName(), item.municipalityState(), d.getServiceCode(),
				d.getDiscrimination(), d.getServiceAmount(), d.getIssRate(), d.getIssAmount(),
				d.getWithholdings().stream().map(w -> w.amount()).reduce(BigDecimal.ZERO, BigDecimal::add),
				d.getAuthorizedAt() != null ? d.getAuthorizedAt() : d.getCreatedAt());
	}
}
