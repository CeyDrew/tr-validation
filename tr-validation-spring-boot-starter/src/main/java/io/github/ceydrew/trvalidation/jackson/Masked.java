package io.github.ceydrew.trvalidation.jackson;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Writes the annotated {@link CharSequence} property to JSON in masked form, so personal data
 * (KVKK) does not leak through API responses or JSON logs.
 *
 * <pre>{@code
 * record CustomerResponse(
 *     @Masked(MaskType.TCKN) String tckn,          // "100******46"
 *     @Masked(MaskType.PHONE_NUMBER) String gsm,   // "+90 532 *** ** 67"
 *     @Masked(MaskType.FULL) String motherName) {} // "*****"
 * }</pre>
 *
 * <p>Takes effect when {@link TrValidationJacksonModule} (Jackson 3) or {@link
 * TrValidationJackson2Module} (Jackson 2) is registered; the Spring Boot starter does that unless
 * {@code tr-validation.masking.enabled=false}. Only serialization is affected: incoming JSON is
 * read unchanged. Using it on a property that is not a {@link CharSequence} fails when the
 * serializer is built.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface Masked {

  /**
   * How to mask the value.
   *
   * @return the mask type
   */
  MaskType value();
}
