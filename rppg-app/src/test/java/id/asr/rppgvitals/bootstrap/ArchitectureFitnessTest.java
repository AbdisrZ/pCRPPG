package id.asr.rppgvitals.bootstrap;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/// Architectural fitness suite enforcing the Dependency-Rule invariants of `04_PACKAGE_STRUCTURE.md §8`.
///
/// This is the secondary, defense-in-depth enforcement layer; the primary enforcement is the Maven
/// module graph (a domain module that never declares JavaFX/OpenCV/ONNX/SQLite makes a cross-layer
/// import a compile error). These rules run inside `mvn verify` and catch what module boundaries
/// alone cannot — sealed-exception placement, package-tree conformance, and forbidden god packages.
///
/// Only production classes are analysed (`DoNotIncludeTests`), and rules whose target types do not
/// yet exist in this Phase-0 scaffold permit an empty match so the suite is green before Phase 1
/// code lands, then tightens automatically as real classes are added.
@AnalyzeClasses(packages = ArchitectureFitnessTest.BASE_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureFitnessTest {

    static final String BASE_PACKAGE = "id.asr.rppgvitals";

    static final String EXCEPTION_ROOT = BASE_PACKAGE + ".domain.exception.RppgApplicationException";

    /// Domain Isolation: no domain class may depend on an infrastructure framework type.
    @ArchTest
    static final ArchRule domainIsMachineFrameworkFree = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("javafx..", "org.opencv..", "ai.onnxruntime..", "org.sqlite..")
            .as("Domain layer must not depend on JavaFX, OpenCV, ONNX Runtime, or SQLite (04 §8)")
            .allowEmptyShould(true);

    /// Application Purity: no application-layer class may depend on JavaFX.
    @ArchTest
    static final ArchRule applicationIsUiFramework = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("javafx..")
            .as("Application layer must not depend on JavaFX (04 §8)")
            .allowEmptyShould(true);

    /// Sealed Exception Closure: every subtype of the exception root lives in `domain.exception`.
    @ArchTest
    static final ArchRule exceptionsResideInDomainExceptionPackage = classes()
            .that()
            .areAssignableTo(EXCEPTION_ROOT)
            .should()
            .resideInAPackage("..domain.exception..")
            .as("All RppgApplicationException subtypes must reside in domain.exception (04 §8)")
            .allowEmptyShould(true);

    /// Package Naming: every production class resides within a package named in the `04 §4` tree.
    @ArchTest
    static final ArchRule packagesMatchTheApprovedTree = classes()
            .should()
            .resideInAnyPackage(
                    "..domain.capture..",
                    "..domain.detection..",
                    "..domain.signal..",
                    "..domain.estimation..",
                    "..domain.session..",
                    "..domain.exception..",
                    "..application.usecase.measurement..",
                    "..application.usecase.history..",
                    "..application.usecase.device..",
                    "..infrastructure.capture.opencv..",
                    "..infrastructure.inference.onnx..",
                    "..infrastructure.persistence.sqlite..",
                    "..presentation.javafx.dashboard..",
                    "..presentation.javafx.history..",
                    "..bootstrap..")
            .as("Every class must reside in a package listed in 04 §4")
            .allowEmptyShould(true);

    /// No God Packages: the catch-all package names forbidden by `00 §20.3` never appear.
    @ArchTest
    static final ArchRule noGodPackages = noClasses()
            .should()
            .resideInAnyPackage("..util..", "..common..", "..misc..")
            .as("Packages named util, common, or misc are forbidden (00 §20.3)")
            .allowEmptyShould(true);
}
