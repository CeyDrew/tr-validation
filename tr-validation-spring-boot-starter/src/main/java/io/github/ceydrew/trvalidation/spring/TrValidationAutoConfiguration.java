package io.github.ceydrew.trvalidation.spring;

import io.github.ceydrew.trvalidation.jackson.TrValidationJackson2Module;
import io.github.ceydrew.trvalidation.jackson.TrValidationJacksonModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for tr-validation.
 *
 * <p>Registers a Jackson module that masks {@link io.github.ceydrew.trvalidation.jackson.Masked}
 * properties and reads and writes tr-validation value types: {@link TrValidationJacksonModule} for
 * Jackson 3 (Spring Boot 4) and {@link TrValidationJackson2Module} for Jackson 2 (Spring Boot 3).
 * Spring Boot adds module beans to its auto-configured mapper. Define your own bean of the same
 * type to replace one.
 *
 * <p>The Bean Validation constraints need no configuration: their messages are found by Hibernate
 * Validator and follow the request locale ({@code Accept-Language}) in Spring MVC.
 */
@AutoConfiguration
@EnableConfigurationProperties(TrValidationProperties.class)
public class TrValidationAutoConfiguration {

  /** Created by Spring Boot. */
  public TrValidationAutoConfiguration() {}

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = "tools.jackson.databind.json.JsonMapper")
  static class Jackson3Configuration {

    @Bean
    @ConditionalOnMissingBean
    TrValidationJacksonModule trValidationJacksonModule(TrValidationProperties properties) {
      return new TrValidationJacksonModule(properties.getMasking().isEnabled());
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
  static class Jackson2Configuration {

    @Bean
    @ConditionalOnMissingBean
    TrValidationJackson2Module trValidationJackson2Module(TrValidationProperties properties) {
      return new TrValidationJackson2Module(properties.getMasking().isEnabled());
    }
  }
}
