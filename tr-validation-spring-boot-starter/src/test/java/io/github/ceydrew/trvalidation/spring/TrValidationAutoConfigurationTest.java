package io.github.ceydrew.trvalidation.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ceydrew.trvalidation.Tckn;
import io.github.ceydrew.trvalidation.jackson.MaskType;
import io.github.ceydrew.trvalidation.jackson.Masked;
import io.github.ceydrew.trvalidation.jackson.TrValidationJackson2Module;
import io.github.ceydrew.trvalidation.jackson.TrValidationJacksonModule;
import jakarta.validation.Validator;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import tools.jackson.databind.json.JsonMapper;

class TrValidationAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  JacksonAutoConfiguration.class,
                  ValidationAutoConfiguration.class,
                  TrValidationAutoConfiguration.class));

  record Customer(@Masked(MaskType.TCKN) String tckn, Tckn typed) {}

  private static final Customer CUSTOMER = new Customer("10000000146", Tckn.of("10000000146"));

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void registersBothModulesAndMasksThroughBootsMapper() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(TrValidationJacksonModule.class);
          assertThat(context).hasSingleBean(TrValidationJackson2Module.class);
          assertThat(context.getBean(TrValidationProperties.class).getMasking().isEnabled())
              .isTrue();

          String json = context.getBean(JsonMapper.class).writeValueAsString(CUSTOMER);
          assertThat(json)
              .isEqualTo("{\"tckn\":\"100******46\",\"typed\":\"100******46\"}")
              .doesNotContain("10000000146");
        });
  }

  @Test
  void maskingCanBeDisabled() {
    runner
        .withPropertyValues("tr-validation.masking.enabled=false")
        .run(
            context -> {
              assertThat(context.getBean(TrValidationProperties.class).getMasking().isEnabled())
                  .isFalse();
              assertThat(context.getBean(JsonMapper.class).writeValueAsString(CUSTOMER))
                  .isEqualTo("{\"tckn\":\"10000000146\",\"typed\":\"10000000146\"}");
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomModules {
    static final TrValidationJacksonModule JACKSON_3 = new TrValidationJacksonModule(false);
    static final TrValidationJackson2Module JACKSON_2 = new TrValidationJackson2Module(false);

    @Bean
    TrValidationJacksonModule customJackson3() {
      return JACKSON_3;
    }

    @Bean
    TrValidationJackson2Module customJackson2() {
      return JACKSON_2;
    }
  }

  @Test
  void backsOffWhenTheApplicationDefinesTheModules() {
    runner
        .withUserConfiguration(CustomModules.class)
        .run(
            context -> {
              assertThat(context.getBean(TrValidationJacksonModule.class))
                  .isSameAs(CustomModules.JACKSON_3);
              assertThat(context.getBean(TrValidationJackson2Module.class))
                  .isSameAs(CustomModules.JACKSON_2);
            });
  }

  @Test
  void skipsJackson3ModuleWithoutJackson3() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TrValidationAutoConfiguration.class))
        .withClassLoader(new FilteredClassLoader("tools.jackson.databind"))
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(TrValidationJacksonModule.class);
              assertThat(context).hasSingleBean(TrValidationJackson2Module.class);
            });
  }

  @Test
  void skipsJackson2ModuleWithoutJackson2() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TrValidationAutoConfiguration.class))
        .withClassLoader(new FilteredClassLoader("com.fasterxml.jackson.databind"))
        .run(
            context -> {
              assertThat(context).hasSingleBean(TrValidationJacksonModule.class);
              assertThat(context).doesNotHaveBean(TrValidationJackson2Module.class);
            });
  }

  record Request(@io.github.ceydrew.trvalidation.jakarta.Tckn String tckn) {}

  @Test
  void bootValidatorUsesTheRequestLocaleForMessages() {
    runner.run(
        context -> {
          Validator validator = context.getBean(Validator.class);
          Request invalid = new Request("10000000147");

          LocaleContextHolder.setLocale(Locale.forLanguageTag("tr-TR"));
          assertThat(validator.validate(invalid).iterator().next().getMessage())
              .isEqualTo("geçerli bir T.C. kimlik numarası olmalıdır");

          LocaleContextHolder.setLocale(Locale.ENGLISH);
          assertThat(validator.validate(invalid).iterator().next().getMessage())
              .isEqualTo("must be a valid Turkish identity number");
        });
  }
}
