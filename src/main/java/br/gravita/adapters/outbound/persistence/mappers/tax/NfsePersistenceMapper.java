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
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfsePersistenceMapper {

	@Mapping(target = "providerMunicipalityIbgeCode", source = "providerMunicipalityIbge")
	@Mapping(target = "tomador", source = "entity")
	@Mapping(target = "issMunicipalityIbgeCode", source = "issMunicipalityIbge")
	@Mapping(target = "createdAt", source = "documentCreatedAt")
	NfseDocument map(final NfseJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "providerCompanyId", source = "providerCompanyId.value")
	@Mapping(target = "providerMunicipalityIbge", source = "providerMunicipalityIbgeCode")
	@Mapping(target = "tomadorPersonId", source = "tomador.personRef.id")
	@Mapping(target = "tomadorDocument", source = "tomador.document.number")
	@Mapping(target = "tomadorPersonType", source = "tomador.document.personType")
	@Mapping(target = "tomadorName", source = "tomador.name")
	@Mapping(target = "tomadorMunicipalityIbge", source = "tomador.municipalityIbgeCode")
	@Mapping(target = "tomadorStreet", source = "tomador.address.street")
	@Mapping(target = "tomadorNumber", source = "tomador.address.number")
	@Mapping(target = "tomadorComplement", source = "tomador.address.complement")
	@Mapping(target = "tomadorNeighborhood", source = "tomador.address.neighborhood")
	@Mapping(target = "tomadorZipCode", source = "tomador.address.zipCode")
	@Mapping(target = "tomadorState", source = "tomador.address.state")
	@Mapping(target = "issMunicipalityIbge", source = "issMunicipalityIbgeCode")
	@Mapping(target = "documentCreatedAt", source = "createdAt")
	@Mapping(target = "createdAt", ignore = true)
	NfseJpaEntity map(final NfseDocument domain);

	@Mapping(target = "personRef", source = "tomadorPersonId")
	@Mapping(target = "document", source = "entity")
	@Mapping(target = "name", source = "tomadorName")
	@Mapping(target = "municipalityIbgeCode", source = "tomadorMunicipalityIbge")
	// An absent address is stored as a null street.
	@Mapping(target = "address", source = "entity", conditionExpression = "java(entity.getTomadorStreet() != null)")
	NfseTomador mapTomador(final NfseJpaEntity entity);

	@Mapping(target = "number", source = "tomadorDocument")
	@Mapping(target = "personType", source = "tomadorPersonType")
	Document mapTomadorDocument(final NfseJpaEntity entity);

	@Mapping(target = "street", source = "tomadorStreet")
	@Mapping(target = "number", source = "tomadorNumber")
	@Mapping(target = "complement", source = "tomadorComplement")
	@Mapping(target = "neighborhood", source = "tomadorNeighborhood")
	@Mapping(target = "zipCode", source = "tomadorZipCode")
	@Mapping(target = "state", source = "tomadorState")
	TomadorAddress mapTomadorAddress(final NfseJpaEntity entity);

	NfseWithholding map(final NfseWithholdingEmbeddable embeddable);

	NfseWithholdingEmbeddable map(final NfseWithholding withholding);

	@Mapping(target = "value", source = "id")
	NfseId mapNfseId(final UUID id);

	@Mapping(target = "value", source = "id")
	CompanyId mapCompanyId(final UUID id);

	@Mapping(target = "id", source = "id")
	PersonRef mapPersonRef(final UUID id);
}
