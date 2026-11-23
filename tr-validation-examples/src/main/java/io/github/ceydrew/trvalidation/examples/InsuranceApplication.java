package io.github.ceydrew.trvalidation.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Small insurance customer registration API showing tr-validation in a Spring Boot application.
 *
 * <p>Nothing here configures tr-validation: adding {@code tr-validation-spring-boot-starter} is
 * enough for the constraints, the Turkish/English messages and JSON masking to work.
 */
@SpringBootApplication
public class InsuranceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InsuranceApplication.class, args);
  }
}
