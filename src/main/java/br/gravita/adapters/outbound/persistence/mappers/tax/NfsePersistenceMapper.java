package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.NfseWithholdingEmbeddable;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.NfseWithholding;
import br.gravita.core.domain.tax.TomadorAddress;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfsePersistenceMapper {

	default NfseDocument toDomain(final NfseJpaEntity entity) {
		return NfseDocument.of(
				NfseId.of(entity.getId()),
				entity.getStatus(),
				CompanyId.of(entity.getProviderCompanyId()),
				entity.getProviderMunicipalityIbge(),
				toTomador(entity),
				entity.getServiceCode(),
				entity.getPlaceOfProvision(),
				entity.getIssMunicipalityIbge(),
				entity.getServiceAmount(),
				entity.getIssRate(),
				entity.getIssAmount(),
				entity.getIssRateOverrideJustification(),
				entity.getWithholdings().stream()
						.map(w -> new NfseWithholding(w.getTaxType(), w.getBase(), w.getRatePercentage(),
								w.getAmount()))
						.toList(),
				entity.getDiscrimination(),
				entity.getRpsSeries(),
				entity.getRpsNumber(),
				entity.getDocumentCreatedAt(),
				entity.getNfseSeries(),
				entity.getNfseNumber(),
				entity.getDraftAt(),
				entity.getSentAt(),
				entity.getProtocol(),
				entity.getAuthorizedAt(),
				entity.getXmlReference(),
				entity.getLastRejectionReason());
	}

	default NfseJpaEntity toEntity(final NfseDocument domain) {
		NfseTomador tomador = domain.getTomador();
		TomadorAddress address = tomador.address();
		return NfseJpaEntity.builder()
				.id(domain.getId().value())
				.status(domain.getStatus())
				.providerCompanyId(domain.getProviderCompanyId().value())
				.providerMunicipalityIbge(domain.getProviderMunicipalityIbgeCode())
				.tomadorPersonId(tomador.personRef() == null ? null : tomador.personRef().id())
				.tomadorDocument(tomador.document().number())
				.tomadorPersonType(tomador.document().personType())
				.tomadorName(tomador.name())
				.tomadorMunicipalityIbge(tomador.municipalityIbgeCode())
				.tomadorStreet(address == null ? null : address.street())
				.tomadorNumber(address == null ? null : address.number())
				.tomadorComplement(address == null ? null : address.complement())
				.tomadorNeighborhood(address == null ? null : address.neighborhood())
				.tomadorZipCode(address == null ? null : address.zipCode())
				.tomadorState(address == null ? null : address.state())
				.serviceCode(domain.getServiceCode())
				.placeOfProvision(domain.getPlaceOfProvision())
				.issMunicipalityIbge(domain.getIssMunicipalityIbgeCode())
				.serviceAmount(domain.getServiceAmount())
				.issRate(domain.getIssRate())
				.issAmount(domain.getIssAmount())
				.issRateOverrideJustification(domain.getIssRateOverrideJustification())
				.discrimination(domain.getDiscrimination())
				.rpsSeries(domain.getRpsSeries())
				.rpsNumber(domain.getRpsNumber())
				.documentCreatedAt(domain.getCreatedAt())
				.nfseSeries(domain.getNfseSeries())
				.nfseNumber(domain.getNfseNumber())
				.draftAt(domain.getDraftAt())
				.sentAt(domain.getSentAt())
				.protocol(domain.getProtocol())
				.authorizedAt(domain.getAuthorizedAt())
				.xmlReference(domain.getXmlReference())
				.lastRejectionReason(domain.getLastRejectionReason())
				.withholdings(toWithholdingEmbeddables(domain.getWithholdings()))
				.build();
	}

	private NfseTomador toTomador(NfseJpaEntity entity) {
		TomadorAddress address = entity.getTomadorStreet() == null ? null
				: new TomadorAddress(entity.getTomadorStreet(), entity.getTomadorNumber(),
						entity.getTomadorComplement(), entity.getTomadorNeighborhood(), entity.getTomadorZipCode(),
						entity.getTomadorState());
		return new NfseTomador(PersonRef.of(entity.getTomadorPersonId()),
				new Document(entity.getTomadorDocument(), entity.getTomadorPersonType()), entity.getTomadorName(),
				entity.getTomadorMunicipalityIbge(), address);
	}

	private List<NfseWithholdingEmbeddable> toWithholdingEmbeddables(List<NfseWithholding> withholdings) {
		List<NfseWithholdingEmbeddable> result = new ArrayList<>();
		for (NfseWithholding w : withholdings) {
			result.add(NfseWithholdingEmbeddable.builder().taxType(w.taxType()).base(w.base())
					.ratePercentage(w.ratePercentage()).amount(w.amount()).build());
		}
		return result;
	}
}
