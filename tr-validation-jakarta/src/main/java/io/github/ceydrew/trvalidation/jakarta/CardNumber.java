package io.github.ceydrew.trvalidation.jakarta;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.ceydrew.trvalidation.CardNetwork;
import io.github.ceydrew.trvalidation.jakarta.internal.CardNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated {@link CharSequence} must be a valid payment card number (Luhn), as checked by
 * {@link io.github.ceydrew.trvalidation.CardNumber#isValid(String)}, optionally limited to some
 * networks.
 *
 * <p>{@code null} is valid; combine with {@code @NotNull} or {@code @NotBlank} to require a value.
 *
 * <pre>{@code
 * record PaymentRequest(@NotNull @CardNumber String pan) {}
 *
 * record DomesticPayment(@CardNumber(networks = CardNetwork.TROY) String pan) {}
 * }</pre>
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface CardNumber {

  /**
   * Networks to accept; empty (the default) accepts any valid number, including {@link
   * CardNetwork#UNKNOWN} networks.
   *
   * @return the accepted networks
   */
  CardNetwork[] networks() default {};

  /**
   * The error message, or a {@code {key}} resolved from the message bundles.
   *
   * @return the message template
   */
  String message() default "{io.github.ceydrew.trvalidation.jakarta.CardNumber.message}";

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
