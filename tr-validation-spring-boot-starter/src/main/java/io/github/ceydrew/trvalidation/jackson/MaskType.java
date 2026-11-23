package io.github.ceydrew.trvalidation.jackson;

import io.github.ceydrew.trvalidation.CardNumber;
import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PostalCode;
import io.github.ceydrew.trvalidation.Tckn;
import io.github.ceydrew.trvalidation.Vkn;
import io.github.ceydrew.trvalidation.Ykn;
import java.util.function.UnaryOperator;

/**
 * How a {@link Masked} value is masked. Each constant delegates to the {@code mask} method of the
 * matching core type, which never reveals more than the documented characters, even for malformed
 * input.
 *
 * <pre>{@code
 * MaskType.TCKN.apply("10000000146");          // "100******46"
 * MaskType.PHONE_NUMBER.apply("05321234567");  // "+90 532 *** ** 67"
 * MaskType.FULL.apply("secret");               // "******"
 * }</pre>
 */
public enum MaskType {
  /** As {@link Tckn#mask(String)}: {@code "100******46"}. */
  TCKN(Tckn::mask),
  /** As {@link Ykn#mask(String)}: {@code "990******42"}. */
  YKN(Ykn::mask),
  /** As {@link Vkn#mask(String)}: {@code "12******90"}. */
  VKN(Vkn::mask),
  /** As {@link Iban#mask(String)}: {@code "TR33 **** **** **** **** **13 26"}. */
  IBAN(Iban::mask),
  /** As {@link CardNumber#mask(String)}: {@code "**** **** **** 1111"}. */
  CARD_NUMBER(CardNumber::mask),
  /** As {@link PhoneNumber#mask(String)}: {@code "+90 532 *** ** 67"}. */
  PHONE_NUMBER(PhoneNumber::mask),
  /** As {@link LicensePlate#mask(String)}: {@code "34 *** *23"}. */
  LICENSE_PLATE(LicensePlate::mask),
  /** As {@link PostalCode#mask(String)}: {@code "34***"}. */
  POSTAL_CODE(PostalCode::mask),
  /** Every character replaced by {@code *}, for any other personal data. */
  FULL(value -> "*".repeat(value.length()));

  private final UnaryOperator<String> masker;

  MaskType(UnaryOperator<String> masker) {
    this.masker = masker;
  }

  /**
   * Masks {@code value}.
   *
   * <pre>{@code
   * MaskType.IBAN.apply("TR330006100519786457841326"); // "TR33 **** **** **** **** **13 26"
   * }</pre>
   *
   * @param value the value, may be {@code null}
   * @return the masked value, or {@code null} if {@code value} is {@code null}
   */
  public String apply(String value) {
    return value == null ? null : masker.apply(value);
  }
}
