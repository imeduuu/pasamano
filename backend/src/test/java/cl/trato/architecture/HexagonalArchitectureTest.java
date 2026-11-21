package cl.trato.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reglas de la arquitectura hexagonal verificadas en CI (RNF-MAN-01). */
@AnalyzeClasses(packages = "cl.trato", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

  /** (1) El dominio no depende de Spring, JPA, Jakarta, Hibernate, Jackson ni infraestructura. */
  @ArchTest
  static final ArchRule domain_is_framework_free =
      noClasses()
          .that()
          .resideInAPackage("cl.trato..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta..",
              "javax.persistence..",
              "org.hibernate..",
              "com.fasterxml..",
              "tools.jackson..",
              "cl.trato..infrastructure..",
              "cl.trato..application..")
          .because("el dominio solo contiene reglas de negocio en Java puro (RNF-MAN-01)")
          .allowEmptyShould(true);

  /** (2) La aplicación no depende de la infraestructura. */
  @ArchTest
  static final ArchRule application_does_not_depend_on_infrastructure =
      noClasses()
          .that()
          .resideInAPackage("cl.trato..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("cl.trato..infrastructure..")
          .because("los casos de uso hablan con el exterior solo a través de puertos")
          .allowEmptyShould(true);

  /** La aplicación tampoco conoce detalles web, de servlet ni de persistencia. */
  @ArchTest
  static final ArchRule application_is_free_of_web_and_persistence_details =
      noClasses()
          .that()
          .resideInAPackage("cl.trato..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "jakarta.persistence..",
              "jakarta.servlet..",
              "org.springframework.web..",
              "org.springframework.data..",
              "org.springframework.security..",
              "org.hibernate..")
          .allowEmptyShould(true);

  /** (3) Un módulo nunca accede a la infraestructura de otro módulo. */
  @ArchTest
  static final ArchRule modules_do_not_access_each_other_through_infrastructure =
      classes()
          .that()
          .resideInAPackage("cl.trato.(*)..")
          .should(notDependOnInfrastructureOfOtherModules())
          .allowEmptyShould(true);

  /** Los controladores y adaptadores no se filtran hacia dentro: el dominio no conoce puertos. */
  @ArchTest
  static final ArchRule infrastructure_in_does_not_depend_on_infrastructure_out =
      noClasses()
          .that()
          .resideInAPackage("cl.trato..infrastructure.in..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("cl.trato..infrastructure.out..")
          .because("los adaptadores de entrada usan casos de uso, no adaptadores de salida")
          .allowEmptyShould(true);

  private static final Pattern MODULE = Pattern.compile("^cl\\.trato\\.([a-z]+)(\\..*)?$");
  private static final Pattern INFRASTRUCTURE =
      Pattern.compile("^cl\\.trato\\.([a-z]+)\\.infrastructure(\\..*)?$");

  /**
   * {@code shared} es el núcleo compartido: su infraestructura (manejo de errores, seguridad base)
   * puede usarse desde los demás módulos.
   */
  private static ArchCondition<JavaClass> notDependOnInfrastructureOfOtherModules() {
    return new ArchCondition<>("not depend on the infrastructure of another module") {
      @Override
      public void check(JavaClass origin, ConditionEvents events) {
        Optional<String> originModule = moduleOf(origin.getPackageName(), MODULE);
        if (originModule.isEmpty()) {
          return;
        }
        for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
          String targetPackage = dependency.getTargetClass().getPackageName();
          Optional<String> targetModule = moduleOf(targetPackage, INFRASTRUCTURE);
          if (targetModule.isPresent()
              && !targetModule.get().equals(originModule.get())
              && !targetModule.get().equals("shared")) {
            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
          }
        }
      }
    };
  }

  private static Optional<String> moduleOf(String packageName, Pattern pattern) {
    Matcher matcher = pattern.matcher(packageName);
    return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
  }
}
