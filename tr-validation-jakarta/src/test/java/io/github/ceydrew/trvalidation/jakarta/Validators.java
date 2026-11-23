package io.github.ceydrew.trvalidation.jakarta;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.HibernateValidator;

/** Hibernate Validator instances for tests, one per message locale. */
final class Validators {

  private static final ValidatorFactory ENGLISH = factory(Locale.ENGLISH);
  private static final ValidatorFactory TURKISH = factory(Locale.forLanguageTag("tr-TR"));

  private Validators() {}

  static Validator english() {
    return ENGLISH.getValidator();
  }

  static Validator turkish() {
    return TURKISH.getValidator();
  }

  /** Validates {@code value} as if assigned to {@code property} of {@code beanType}. */
  static <T> boolean isValid(Class<T> beanType, String property, Object value) {
    return english().validateValue(beanType, property, value).isEmpty();
  }

  static <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
    return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
  }

  /** A new factory, so no message bundle cached by another factory is reused. */
  static ValidatorFactory factory(Locale locale) {
    return Validation.byProvider(HibernateValidator.class)
        .configure()
        .defaultLocale(locale)
        .buildValidatorFactory();
  }
}
