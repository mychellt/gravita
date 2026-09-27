package br.gravita.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.gravita", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domain_should_not_depend_on_application_or_adapters =
			noClasses().that().resideInAnyPackage("..domain..")
					.should().dependOnClassesThat().resideInAnyPackage(
							"..ports..", "..usercases..", "..application..", "..adapter..", "..adapters..");

	@ArchTest
	static final ArchRule application_should_not_depend_on_adapters =
			noClasses().that().resideInAnyPackage("..core..", "..application..")
					.and().resideOutsideOfPackage("..adapter..")
					.and().resideOutsideOfPackage("..adapters..")
					.should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..adapters..");

	@ArchTest
	static final ArchRule domain_should_be_free_of_web_and_persistence_frameworks =
			noClasses().that().resideInAnyPackage("..domain..")
					.should().dependOnClassesThat().resideInAnyPackage(
							"org.springframework..", "jakarta.persistence..", "jakarta.validation..");
}
