package br.gravita.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.gravita", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Hexagonal architecture rules: domain and application layers stay independent of adapters and frameworks")
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule DOMAIN_SHOULD_NOT_DEPEND_ON_APPLICATION_OR_ADAPTERS =
			noClasses().that().resideInAnyPackage("..domain..")
					.should().dependOnClassesThat().resideInAnyPackage(
							"..ports..", "..usercases..", "..application..", "..adapter..", "..adapters..");

	@ArchTest
	static final ArchRule APPLICATION_SHOULD_NOT_DEPEND_ON_ADAPTERS =
			noClasses().that().resideInAnyPackage("..core..", "..application..")
					.and().resideOutsideOfPackage("..adapter..")
					.and().resideOutsideOfPackage("..adapters..")
					.should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..adapters..");

	@ArchTest
	static final ArchRule DOMAIN_SHOULD_BE_FREE_OF_WEB_AND_PERSISTENCE_FRAMEWORKS =
			noClasses().that().resideInAnyPackage("..domain..")
					.should().dependOnClassesThat().resideInAnyPackage(
							"org.springframework..", "jakarta.persistence..", "jakarta.validation..");
}
