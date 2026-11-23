package io.github.ceydrew.trvalidation.jakarta;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.ceydrew.trvalidation.jakarta.internal.TrPostalCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated {@link CharSequence} must be a structurally valid Turkish postal code, as checked
 * by {@link io.github.ceydrew.trvalidation.PostalCode#isValid(String)}.
 *
 * <p>{@code null} is valid; combine with {@code @NotNull} or {@code @NotBlank} to require a value.
 *
 * <pre>{@code
 * record Address(@NotNull @TrPostalCode String postalCode) {}
 * }</pre>
 */
@Documented
@Constraint(validatedBy = TrPostalCodeValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface TrPostalCode {

  /**
   * The error message, or a {@code {key}} resolved from the message bundles.
   *
   * @return the message template
   */
  String message() default "{io.github.ceydrew.trvalidation.jakarta.TrPostalCode.message}";

  /**
   * The validation groups this constraint belongs to.
   *
   * @return the groups
   */
  Class<?>[] groups() default {};

  /**
   * Payload for clients of the Bean Validation API.
   *
   * @return the payload
   */
  Class<? extends Payload>[] payload() default {};
}
