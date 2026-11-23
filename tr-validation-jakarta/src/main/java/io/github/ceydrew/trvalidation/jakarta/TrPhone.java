package io.github.ceydrew.trvalidation.jakarta;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.ceydrew.trvalidation.jakarta.internal.TrPhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated {@link CharSequence} must be a valid Turkish phone number, as checked by {@link
 * io.github.ceydrew.trvalidation.PhoneNumber#isValid(String)}. Common forms such as {@code +90 532
 * 123 45 67}, {@code 0532 123 45 67} and {@code 5321234567} are accepted.
 *
 * <p>{@code null} is valid; combine with {@code @NotNull} or {@code @NotBlank} to require a value.
 *
 * <pre>{@code
 * record ContactRequest(@TrPhone String phone) {}
 *
 * record OtpRequest(@NotNull @TrPhone(mobileOnly = true) String gsm) {}
 * }</pre>
 */
@Documented
@Constraint(validatedBy = TrPhoneValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface TrPhone {

  /**
   * Accepts only mobile numbers ({@link io.github.ceydrew.trvalidation.PhoneType#MOBILE}).
   *
   * @return {@code true} to reject fixed-line and non-geographic numbers
   */
  boolean mobileOnly() default false;

  /**
   * The error message, or a {@code {key}} resolved from the message bundles.
   *
   * @return the message template
   */
  String message() default "{io.github.ceydrew.trvalidation.jakarta.TrPhone.message}";

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
