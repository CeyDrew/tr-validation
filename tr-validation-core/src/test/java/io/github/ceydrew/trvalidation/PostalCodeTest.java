package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PostalCodeTest {

  @ParameterizedTest
  @ValueSource(strings = {"34710", "01000", "81999", "06 100", " 35-000 "})
  void acceptsValidCodes(String input) {
    assertThat(PostalCode.isValid(input)).isTrue();
    assertThat(PostalCode.validate(input)).isSameAs(ValidationResult.valid());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void rejectsEmptyInput(String input) {
    assertThat(PostalCode.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "3471A, INVALID_CHARACTER",
    "34.710, INVALID_CHARACTER",
    "3471, WRONG_LENGTH",
    "347100, WRONG_LENGTH",
    "00100, INVALID_PROVINCE_CODE",
    "82000, INVALID_PROVINCE_CODE",
    "99999, INVALID_PROVINCE_CODE"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(PostalCode.isValid(input)).isFalse();
    assertThat(PostalCode.validate(input).invalidReason()).contains(reason);
  }

  @Test
  void normalizesAndMasks() {
    assertThat(PostalCode.normalize(" 34 710 ")).isEqualTo("34710");
    assertThat(PostalCode.normalize(null)).isNull();
    assertThat(PostalCode.mask("34710")).isEqualTo("34***");
    assertThat(PostalCode.mask("347")).isEqualTo("***");
    assertThat(PostalCode.mask(null)).isNull();
  }

  @Test
  void ofParseAndAccessors() {
    PostalCode code = PostalCode.of("06 100");

    assertThat(code.value()).isEqualTo("06100");
    assertThat(code.provinceCode()).isEqualTo(6);
    assertThat(code.masked()).isEqualTo("06***");
    assertThat(code).hasToString("06100");
    assertThat(PostalCode.parse("06100")).contains(code);
    assertThat(PostalCode.parse("abc")).isEmpty();
    assertThat(code)
        .isEqualTo(code)
        .hasSameHashCodeAs(PostalCode.of("06100"))
        .isNotEqualTo(PostalCode.of("34710"))
        .isNotEqualTo("06100")
        .isNotEqualTo(null);
    assertThatThrownBy(() -> PostalCode.of("99999"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid postal code: INVALID_PROVINCE_CODE");
  }

  @Test
  void randomIsValidEvenUnderNonLatinDefaultLocale() {
    Locale original = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"));
      SplittableRandom rng = new SplittableRandom(42);
      for (int i = 0; i < 10_000; i++) {
        String value = PostalCode.random(rng).value();
        assertThat(PostalCode.isValid(value)).as(value).isTrue();
      }
      assertThat(PostalCode.isValid(PostalCode.random().value())).isTrue();
    } finally {
      Locale.setDefault(original);
    }
  }
}
