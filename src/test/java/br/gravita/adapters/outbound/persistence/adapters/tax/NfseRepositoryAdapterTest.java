package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalServiceCodeJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.ServiceTaxRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfsePersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalServiceCodeJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.ServiceTaxRuleJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseNumber;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.NfseWithholding;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.domain.tax.ServiceTaxRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TomadorAddress;
import br.gravita.core.domain.tax.WithholdingMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({ NfseRepositoryAdapter.class, NfsePersistenceMapperImpl.class, ServiceTaxRuleRepositoryAdapter.class,
		MunicipalServiceCodeRepositoryAdapter.class })
class NfseRepositoryAdapterTest {

	@Autowired
	private NfseRepositoryAdapter nfseRepositoryAdapter;
	@Autowired
	private ServiceTaxRuleRepositoryAdapter ruleRepositoryAdapter;
	@Autowired
	private MunicipalServiceCodeRepositoryAdapter municipalServiceCodeRepositoryAdapter;
	@Autowired
	private NfseJpaRepository nfseJpaRepository;
	@Autowired
	private ServiceTaxRuleJpaRepository ruleJpaRepository;
	@Autowired
	private MunicipalServiceCodeJpaRepository municipalServiceCodeJpaRepository;

	private static NfseDocument rps(NfseTomador tomador, List<NfseWithholding> withholdings, String justification) {
		return NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()), "3550308",
				tomador, ServiceCode.of("1.05"), PlaceOfProvision.RECIPIENT, "3304557", new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal("50.00"), justification, withholdings,
				"Desenvolvimento de software sob demanda", "RPS1", 42L, Instant.parse("2026-10-01T10:00:00Z"));
	}

	@Test
	void roundTripsAnRpsWithItsTomadorAddressAndWithholdings() {
		NfseTomador tomador = NfseTomador.of(PersonRef.of(UUID.randomUUID()), "11222333000181", PersonType.COMPANY,
				"Tomador SA", "3304557", new TomadorAddress("Rua A", "10", "Sala 2", "Centro", "20000000", "RJ"));
		NfseDocument saved = nfseRepositoryAdapter.save(rps(tomador,
				List.of(new NfseWithholding(TaxType.ISS, new BigDecimal("1000.00"), new BigDecimal("5.0000"),
						new BigDecimal("50.00")),
						new NfseWithholding(TaxType.CSLL, new BigDecimal("1000.00"), new BigDecimal("1.0000"),
								new BigDecimal("10.00"))),
				"Beneficio"));
		nfseJpaRepository.flush();

		NfseDocument found = nfseRepositoryAdapter.findById(saved.getId()).orElseThrow();

		assertThat(found.getStatus()).isEqualTo(NfseStatus.RPS);
		assertThat(found.getRpsSeries()).isEqualTo("RPS1");
		assertThat(found.getRpsNumber()).isEqualTo(42L);
		assertThat(found.getServiceCode()).isEqualTo("01.05");
		assertThat(found.getPlaceOfProvision()).isEqualTo(PlaceOfProvision.RECIPIENT);
		assertThat(found.getIssMunicipalityIbgeCode()).isEqualTo("3304557");
		assertThat(found.getIssRate()).isEqualByComparingTo("5.0000");
		assertThat(found.getIssRateOverrideJustification()).isEqualTo("Beneficio");
		assertThat(found.getDiscrimination()).isEqualTo("Desenvolvimento de software sob demanda");
		assertThat(found.getTomador().document().number()).isEqualTo("11222333000181");
		assertThat(found.getTomador().isCompany()).isTrue();
		assertThat(found.getTomador().hasFullAddress()).isTrue();
		assertThat(found.getTomador().address().complement()).isEqualTo("Sala 2");
		assertThat(found.getWithholdings()).extracting(NfseWithholding::taxType)
				.containsExactlyInAnyOrder(TaxType.ISS, TaxType.CSLL);
		assertThat(found.getCreatedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
	}

	@Test
	void roundTripsTheNfseSeriesNumberAndDraftTimestampOfAConvertedDocument() {
		NfseTomador tomador = NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);
		NfseDocument rps = nfseRepositoryAdapter.save(rps(tomador, List.of(), null));
		nfseJpaRepository.flush();

		nfseRepositoryAdapter.save(rps.convertToNfse("1", 9L, Instant.parse("2026-10-01T12:00:00Z")));
		nfseJpaRepository.flush();

		NfseDocument found = nfseRepositoryAdapter.findByIdForUpdate(rps.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(found.getNfseSeries()).isEqualTo("1");
		assertThat(found.getNfseNumber()).isEqualTo(9L);
		assertThat(found.getDraftAt()).isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
		assertThat(found.getRpsNumber()).isEqualTo(42L);
		assertThat(nfseRepositoryAdapter.findByIdForUpdate(NfseId.of(UUID.randomUUID()))).isEmpty();
	}

	@Test
	void nfseNumbersAreSequentialPerCompanyAndMunicipality() {
		CompanyId a = CompanyId.of(UUID.randomUUID());
		CompanyId b = CompanyId.of(UUID.randomUUID());

		assertThat(nfseRepositoryAdapter.allocateNextNumber(a, "3550308")).isEqualTo(new NfseNumber("1", 1L));
		assertThat(nfseRepositoryAdapter.allocateNextNumber(a, "3550308")).isEqualTo(new NfseNumber("1", 2L));
		assertThat(nfseRepositoryAdapter.allocateNextNumber(a, "3304557")).isEqualTo(new NfseNumber("1", 1L));
		assertThat(nfseRepositoryAdapter.allocateNextNumber(b, "3550308")).isEqualTo(new NfseNumber("1", 1L));
		assertThat(nfseRepositoryAdapter.allocateNextNumber(a, "3550308")).isEqualTo(new NfseNumber("1", 3L));
	}

	@Test
	void roundTripsAPfTomadorWithoutAddressOrWithholdings() {
		NfseTomador tomador = NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);
		NfseDocument saved = nfseRepositoryAdapter.save(rps(tomador, List.of(), null));
		nfseJpaRepository.flush();

		NfseDocument found = nfseRepositoryAdapter.findById(saved.getId()).orElseThrow();

		assertThat(found.getTomador().address()).isNull();
		assertThat(found.getTomador().municipalityIbgeCode()).isNull();
		assertThat(found.getTomador().personRef()).isNull();
		assertThat(found.getWithholdings()).isEmpty();
		assertThat(found.isIssRateOverridden()).isFalse();
		assertThat(nfseRepositoryAdapter.findById(NfseId.of(UUID.randomUUID()))).isEmpty();
	}

	@Test
	void findCandidatesReturnsTheMunicipalityRowsAndTheWildcardOnesOfThatServiceOnly() {
		saveRule("01.05", "3550308", TaxRegime.LUCRO_PRESUMIDO, TaxType.ISS, "5.0000", WithholdingMode.TOMADOR_COMPANY);
		saveRule("01.05", null, null, TaxType.PIS, "0.6500", WithholdingMode.ALWAYS);
		saveRule("01.05", "3304557", null, TaxType.ISS, "3.0000", WithholdingMode.NEVER);
		saveRule("02.01", "3550308", null, TaxType.ISS, "2.0000", WithholdingMode.NEVER);
		ruleJpaRepository.flush();

		List<ServiceTaxRule> candidates = ruleRepositoryAdapter.findCandidates("01.05", "3550308");

		assertThat(candidates).extracting(ServiceTaxRule::taxType).containsExactlyInAnyOrder(TaxType.ISS, TaxType.PIS);
		ServiceTaxRule iss = candidates.stream().filter(r -> r.taxType() == TaxType.ISS).findFirst().orElseThrow();
		assertThat(iss.municipalityIbgeCode()).isEqualTo("3550308");
		assertThat(iss.regime()).isEqualTo(TaxRegime.LUCRO_PRESUMIDO);
		assertThat(iss.ratePercentage()).isEqualByComparingTo("5.0000");
		assertThat(iss.withholding()).isEqualTo(WithholdingMode.TOMADOR_COMPANY);
	}

	@Test
	void municipalServiceListLookups() {
		MunicipalServiceCodeJpaEntity entity = MunicipalServiceCodeJpaEntity.builder().id(UUID.randomUUID())
				.municipalityIbge("3550308").serviceCode("01.05").build();
		municipalServiceCodeJpaRepository.saveAndFlush(entity);

		assertThat(municipalServiceCodeRepositoryAdapter.hasServiceCodeList("3550308")).isTrue();
		assertThat(municipalServiceCodeRepositoryAdapter.existsByMunicipalityAndServiceCode("3550308", "01.05"))
				.isTrue();
		assertThat(municipalServiceCodeRepositoryAdapter.existsByMunicipalityAndServiceCode("3550308", "02.01"))
				.isFalse();
		assertThat(municipalServiceCodeRepositoryAdapter.hasServiceCodeList("3304557")).isFalse();
	}

	private void saveRule(String serviceCode, String municipality, TaxRegime regime, TaxType type, String rate,
			WithholdingMode mode) {
		ruleJpaRepository.save(ServiceTaxRuleJpaEntity.builder().id(UUID.randomUUID()).serviceCode(serviceCode)
				.municipalityIbge(municipality).regime(regime).taxType(type).ratePercentage(new BigDecimal(rate))
				.withholding(mode).build());
	}
}
