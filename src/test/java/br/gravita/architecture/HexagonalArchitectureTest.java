package br.gravita.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the ports-and-adapters boundary between {@code br.gravita.core}
 * (domain, ports, use cases) and {@code br.gravita.adapters} (web, JPA, ...).
 */
@AnalyzeClasses(packages = "br.gravita", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domain_should_not_depend_on_ports_or_usecases =
			noClasses().that().resideInAPackage("..core.domain..")
					.should().dependOnClassesThat().resideInAnyPackage("..core.ports..", "..core.usercases..");

	@ArchTest
	static final ArchRule core_should_not_depend_on_adapters =
			noClasses().that().resideInAPackage("..core..")
					.should().dependOnClassesThat().resideInAPackage("..adapters..");

	@ArchTest
	static final ArchRule domain_should_be_free_of_web_and_persistence_frameworks =
			noClasses().that().resideInAPackage("..core.domain..")
					.should().dependOnClassesThat().resideInAnyPackage(
							"org.springframework..", "jakarta.persistence..", "jakarta.validation..");
}
