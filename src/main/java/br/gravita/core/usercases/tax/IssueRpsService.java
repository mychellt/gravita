package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.RpsId;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.domain.tax.ServiceTaxRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TomadorAddress;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand.AddressCommand;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand.TomadorCommand;
import br.gravita.core.ports.inbound.tax.IssueRpsUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalServiceCodeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.ServiceTaxRuleRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UC-M4-02. Issues an internal RPS: validates the service code, resolves the ISS rate and the withholdings from the
 * parameterized service-tax table (never hardcoded per call), numbers it from the company's own {@code RPS}
 * document series and stores it as a pre-conversion {@link NfseDocument}. Nothing is transmitted - that is
 * UC-M4-03/04.
 */
@UseCase
public class IssueRpsService implements IssueRpsUseCase {

	private static final BigDecimal MAX_RATE = BigDecimal.valueOf(100);

	private final NfseRepositoryPort nfseRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final ServiceTaxRuleRepositoryPort serviceTaxRuleRepositoryPort;
	private final MunicipalServiceCodeRepositoryPort municipalServiceCodeRepositoryPort;
	private final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;
	private final ServiceTaxCalculator serviceTaxCalculator = new ServiceTaxCalculator(new TaxEngine());

	public IssueRpsService(final NfseRepositoryPort nfseRepositoryPort, final CompanyRepositoryPort companyRepositoryPort,
			final ServiceTaxRuleRepositoryPort serviceTaxRuleRepositoryPort,
			final MunicipalServiceCodeRepositoryPort municipalServiceCodeRepositoryPort,
			final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase) {
		this.nfseRepositoryPort = nfseRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.serviceTaxRuleRepositoryPort = serviceTaxRuleRepositoryPort;
		this.municipalServiceCodeRepositoryPort = municipalServiceCodeRepositoryPort;
		this.allocateDocumentNumberUseCase = allocateDocumentNumberUseCase;
	}

	@Override
	public RpsId execute(final IssueRpsCommand command) {
		final Company company = companyRepositoryPort.findById(CompanyId.of(command.providerCompanyId()))
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + command.providerCompanyId()));

		final NfseTomador tomador = buildTomador(command.tomador());
		final String issMunicipality = resolveIssMunicipality(command, tomador);

		// AC1: LC 116/2003 list, then the list of the municipality the service is taxed in.
		final ServiceCode serviceCode = ServiceCode.of(command.serviceCode());
		validateAgainstMunicipalList(serviceCode, issMunicipality);

		// AC3: a manual ISS rate is only accepted together with a justification.
		final BigDecimal issRateOverride = validateOverride(command);

		// AC3/AC4: rate and withholdings come from the rule table, applied by the shared engine.
		final TaxRegime regime = TaxRegime.valueOf(company.getTaxRegime().name());
		final List<ServiceTaxRule> candidates = serviceTaxRuleRepositoryPort.findCandidates(serviceCode.value(),
				issMunicipality);
		final ServiceTaxCalculator.Result tax = serviceTaxCalculator.calculate(candidates, serviceCode.value(),
				issMunicipality, regime, company.getState(), tomador, command.serviceAmount(), issRateOverride);

		// AC2: checked before a number is allocated, so a rejected RPS never burns one.
		NfseDocument.requireFullAddressIfWithheld(tomador, tax.withholdings());

		// AC5: the RPS has its own series/number, independent of the NFe series.
		final DocumentNumber number = allocateDocumentNumberUseCase
				.execute(new AllocateDocumentNumberCommand(company.getId(), FiscalDocumentType.RPS));
		final NfseDocument rps = NfseDocument.issueRps()
				.id(NfseId.of(UUID.randomUUID()))
				.providerCompanyId(company.getId())
				.providerMunicipalityIbgeCode(command.providerMunicipalityIbgeCode())
				.tomador(tomador)
				.serviceCode(serviceCode)
				.placeOfProvision(command.placeOfProvision())
				.issMunicipalityIbgeCode(issMunicipality)
				.serviceAmount(command.serviceAmount())
				.issRate(tax.issRate())
				.issAmount(tax.issAmount())
				.issRateOverrideJustification(issRateOverride == null ? null : command.overrideJustification())
				.withholdings(tax.withholdings())
				.discrimination(command.discrimination())
				.rpsSeries(number.series())
				.rpsNumber(number.number())
				.createdAt(Instant.now())
				.build();

		return nfseRepositoryPort.save(rps).getRpsId();
	}

	private NfseTomador buildTomador(final TomadorCommand tomador) {
		final AddressCommand address = tomador.address();
		final TomadorAddress tomadorAddress = address == null ? null
				: new TomadorAddress(address.street(), address.number(), address.complement(),
						address.neighborhood(), address.zipCode(), address.state());
		return NfseTomador.of(PersonRef.of(tomador.personId()), tomador.document(), tomador.personType(),
				tomador.name(), tomador.municipalityIbgeCode(), tomadorAddress);
	}

	/** Place of provision decides which municipality ISS is due to (doc §5.2). */
	private String resolveIssMunicipality(final IssueRpsCommand command, final NfseTomador tomador) {
		if (command.placeOfProvision() == PlaceOfProvision.PROVIDER) {
			return command.providerMunicipalityIbgeCode();
		}
		if (tomador.municipalityIbgeCode() == null) {
			throw new BusinessRuleException(
					"Tomador municipalityIbgeCode is required when the place of provision is the recipient's");
		}
		return tomador.municipalityIbgeCode();
	}

	/**
	 * A municipality with its own service-code list only accepts the codes on it; one with no list configured falls
	 * back to the LC 116/2003 list alone.
	 */
	private void validateAgainstMunicipalList(final ServiceCode serviceCode, final String municipalityIbgeCode) {
		if (municipalServiceCodeRepositoryPort.hasServiceCodeList(municipalityIbgeCode)
				&& !municipalServiceCodeRepositoryPort.existsByMunicipalityAndServiceCode(municipalityIbgeCode,
						serviceCode.value())) {
			throw new BusinessRuleException("Service code " + serviceCode.value()
					+ " is not on the service list of municipality " + municipalityIbgeCode);
		}
	}

	private BigDecimal validateOverride(final IssueRpsCommand command) {
		final BigDecimal override = command.issRateOverride();
		if (override == null) {
			return null;
		}
		if (command.overrideJustification() == null || command.overrideJustification().isBlank()) {
			throw new BusinessRuleException("A manual ISS rate override requires a justification");
		}
		if (override.signum() < 0 || override.compareTo(MAX_RATE) > 0) {
			throw new BusinessRuleException("ISS rate override must be between 0 and 100: " + override);
		}
		return override;
	}
}
