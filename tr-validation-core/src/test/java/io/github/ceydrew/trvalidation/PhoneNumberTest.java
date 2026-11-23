package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "+905321234567",
        "+90 532 123 45 67",
        "+90 (532) 123-45-67",
        "+90 0532 123 45 67",
        "+90(0532)1234567",
        "0090 532 123 45 67",
        "0090 0532 123 45 67",
        "90 532 123 45 67",
        "905321234567",
        "0532 123 45 67",
        "(0532) 123 45 67",
        "05321234567",
        "5321234567",
        "532.123.45.67",
        "532-123-4567"
      })
  void acceptsAllCommonFormsOfTheSameNumber(String input) {
    assertThat(PhoneNumber.isValid(input)).isTrue();
    assertThat(PhoneNumber.validate(input)).isSameAs(ValidationResult.valid());
    assertThat(PhoneNumber.normalize(input)).isEqualTo("+905321234567");
    assertThat(PhoneNumber.type(input)).contains(PhoneType.MOBILE);
  }

  @ParameterizedTest
  @CsvSource({
    "0212 123 45 67, +902121234567, FIXED_LINE",
    "0312 123 45 67, +903121234567, FIXED_LINE",
    "0462 123 45 67, +904621234567, FIXED_LINE",
    "0800 123 45 67, +908001234567, NON_GEOGRAPHIC",
    "0850 123 45 67, +908501234567, NON_GEOGRAPHIC",
    "0900 123 45 67, +909001234567, NON_GEOGRAPHIC",
    "9001234567, +909001234567, NON_GEOGRAPHIC",
    "90 900 123 45 67, +909001234567, NON_GEOGRAPHIC",
    "0501 123 45 67, +905011234567, MOBILE",
    "0555 123 45 67, +905551234567, MOBILE"
  })
  void classifiesNumbers(String input, String e164, PhoneType type) {
    assertThat(PhoneNumber.normalize(input)).isEqualTo(e164);
    assertThat(PhoneNumber.type(input)).contains(type);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "()", "+", "- . -"})
  void rejectsEmptyInput(String input) {
    assertThat(PhoneNumber.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "0532 123 45 6x, INVALID_CHARACTER",
    "++90 532 123 45 67, INVALID_CHARACTER",
    "90+532 123 45 67, INVALID_CHARACTER",
    "0532/123/45/67, INVALID_CHARACTER",
    "532 123 456, WRONG_LENGTH",
    "0532 123 45 678, WRONG_LENGTH",
    "+90 532 123 45 678, WRONG_LENGTH",
    "+90 00532 123 45 67, WRONG_LENGTH",
    "90 0532 123 45 67, WRONG_LENGTH",
    "0090 532 123 45 678, WRONG_LENGTH",
    "915321234567, WRONG_LENGTH",
    "+90 1532 123 45 67, WRONG_LENGTH",
    "+1 202 555 0100, INVALID_COUNTRY_CODE",
    "+91 532 123 45 67, INVALID_COUNTRY_CODE",
    "+44 20 7946 0000, INVALID_COUNTRY_CODE",
    "0044 20 7946 0000, INVALID_COUNTRY_CODE",
    "0091 532 123 45 67, INVALID_COUNTRY_CODE",
    "0123 456 78 90, INVALID_PREFIX",
    "0612 345 67 89, INVALID_PREFIX",
    "0712 345 67 89, INVALID_PREFIX",
    "0811 123 45 67, INVALID_PREFIX",
    "0999 123 45 67, INVALID_PREFIX",
    "+90 1234 567 890, INVALID_PREFIX"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(PhoneNumber.isValid(input)).isFalse();
    assertThat(PhoneNumber.validate(input).invalidReason()).contains(reason);
    assertThat(PhoneNumber.normalize(input)).isNull();
    assertThat(PhoneNumber.format(input)).isNull();
    assertThat(PhoneNumber.type(input)).isEmpty();
  }

  @Test
  void formatsAndMasks() {
    assertThat(PhoneNumber.format("05321234567")).isEqualTo("+90 532 123 45 67");
    assertThat(PhoneNumber.format("(0212) 123 4567")).isEqualTo("+90 212 123 45 67");
    assertThat(PhoneNumber.mask("0532 123 45 67")).isEqualTo("+90 532 *** ** 67");
    assertThat(PhoneNumber.mask("12345")).isEqualTo("*****");
    assertThat(PhoneNumber.mask("")).isEmpty();
    assertThat(PhoneNumber.mask(null)).isNull();
    assertThat(PhoneNumber.normalize(null)).isNull();
  }

  @Test
  void ofParseAndAccessors() {
    PhoneNumber phone = PhoneNumber.of("0532 123 45 67");

    assertThat(phone.value()).isEqualTo("+905321234567");
    assertThat(phone.type()).isEqualTo(PhoneType.MOBILE);
    assertThat(phone.formatted()).isEqualTo("+90 532 123 45 67");
    assertThat(phone.masked()).isEqualTo("+90 532 *** ** 67");
    assertThat(phone).hasToString("+90 532 *** ** 67");
    assertThat(PhoneNumber.parse("+90 532 123 45 67")).contains(phone);
    assertThat(PhoneNumber.parse("911")).isEmpty();
    assertThat(phone)
        .isEqualTo(phone)
        .hasSameHashCodeAs(PhoneNumber.of("5321234567"))
        .isNotEqualTo(PhoneNumber.of("0212 123 45 67"))
        .isNotEqualTo("+905321234567")
        .isNotEqualTo(null);
    assertThatThrownBy(() -> PhoneNumber.of("+44 20 7946 0000"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid phone number: INVALID_COUNTRY_CODE")
        .hasMessageNotContaining("7946");
  }
}
