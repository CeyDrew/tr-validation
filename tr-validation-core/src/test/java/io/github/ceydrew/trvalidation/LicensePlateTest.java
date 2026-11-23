package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

  @ParameterizedTest
  @CsvSource({
    "34 A 1234, 34 A 1234",
    "06 AB 123, 06 AB 123",
    "06 AB 1234, 06 AB 1234",
    "35 ABC 12, 35 ABC 12",
    "35 ABC 123, 35 ABC 123",
    "34abc123, 34 ABC 123",
    "34-ABC-123, 34 ABC 123",
    "01 a 0001, 01 A 0001",
    "81 Z 9999, 81 Z 9999"
  })
  void acceptsValidPlatesAndFormats(String input, String formatted) {
    assertThat(LicensePlate.isValid(input)).isTrue();
    assertThat(LicensePlate.validate(input)).isSameAs(ValidationResult.valid());
    assertThat(LicensePlate.format(input)).isEqualTo(formatted);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "--"})
  void rejectsEmptyInput(String input) {
    assertThat(LicensePlate.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "34 QA 123, INVALID_CHARACTER",
    "34 AW 123, INVALID_CHARACTER",
    "34 x 1234, INVALID_CHARACTER",
    "34 ÇA 123, INVALID_CHARACTER",
    "34 AB 12.3, INVALID_CHARACTER",
    "00 A 1234, INVALID_PROVINCE_CODE",
    "82 A 1234, INVALID_PROVINCE_CODE",
    "99 AB 123, INVALID_PROVINCE_CODE",
    "ABC 123, INVALID_FORMAT",
    "3 A 1234, INVALID_FORMAT",
    "345 A 1234, INVALID_FORMAT",
    "34, INVALID_FORMAT",
    "341234, INVALID_FORMAT",
    "34 A 123, INVALID_FORMAT",
    "34 A 12345, INVALID_FORMAT",
    "34 AB 12, INVALID_FORMAT",
    "34 AB 12345, INVALID_FORMAT",
    "34 ABC 1, INVALID_FORMAT",
    "34 ABC 1234, INVALID_FORMAT",
    "34 ABCD 12, INVALID_FORMAT",
    "34 AB, INVALID_FORMAT",
    "34 AB 123 C, INVALID_FORMAT"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(LicensePlate.isValid(input)).isFalse();
    assertThat(LicensePlate.validate(input).invalidReason()).contains(reason);
    assertThat(LicensePlate.format(input)).isNull();
  }

  @Test
  void normalizesAndMasks() {
    assertThat(LicensePlate.normalize("34 abc-123")).isEqualTo("34ABC123");
    assertThat(LicensePlate.normalize(null)).isNull();
    assertThat(LicensePlate.mask("34 ABC 123")).isEqualTo("34 *** *23");
    assertThat(LicensePlate.mask("34 A 1234")).isEqualTo("34 * **34");
    assertThat(LicensePlate.mask("35 abc 12")).isEqualTo("35 *** 12");
    assertThat(LicensePlate.mask("XYZ")).isEqualTo("***");
    assertThat(LicensePlate.mask(null)).isNull();
  }

  @Test
  void ofParseAndAccessors() {
    LicensePlate plate = LicensePlate.of("06 ab 1234");

    assertThat(plate.value()).isEqualTo("06AB1234");
    assertThat(plate.provinceCode()).isEqualTo(6);
    assertThat(plate.formatted()).isEqualTo("06 AB 1234");
    assertThat(plate.masked()).isEqualTo("06 ** **34");
    assertThat(plate).hasToString("06 ** **34");
    assertThat(LicensePlate.parse("06AB1234")).contains(plate);
    assertThat(LicensePlate.parse("06 AB 1")).isEmpty();
    assertThat(plate)
        .isEqualTo(plate)
        .hasSameHashCodeAs(LicensePlate.of("06-AB-1234"))
        .isNotEqualTo(LicensePlate.of("06 AB 123"))
        .isNotEqualTo("06AB1234")
        .isNotEqualTo(null);
    assertThatThrownBy(() -> LicensePlate.of("82 A 1234"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid license plate: INVALID_PROVINCE_CODE")
        .hasMessageNotContaining("1234");
  }

  @Test
  void randomIsValid() {
    SplittableRandom rng = new SplittableRandom(42);
    for (int i = 0; i < 10_000; i++) {
      LicensePlate plate = LicensePlate.random(rng);
      assertThat(LicensePlate.isValid(plate.value())).as(plate.value()).isTrue();
      assertThat(LicensePlate.of(plate.formatted())).isEqualTo(plate);
    }
    assertThat(LicensePlate.isValid(LicensePlate.random().value())).isTrue();
  }
}
