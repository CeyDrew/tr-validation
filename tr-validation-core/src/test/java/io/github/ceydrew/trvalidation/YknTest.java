package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class YknTest {

  @ParameterizedTest
  @ValueSource(strings = {"99000000042", "99123456740", "990 000 000 42", "990-000-000-42"})
  void acceptsValidNumbers(String input) {
    assertThat(Ykn.isValid(input)).isTrue();
    assertThat(Ykn.validate(input)).isSameAs(ValidationResult.valid());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void rejectsEmptyInput(String input) {
    assertThat(Ykn.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "9900000004x, INVALID_CHARACTER",
    "9900000004, WRONG_LENGTH",
    "10000000146, INVALID_PREFIX",
    "98000000000, INVALID_PREFIX",
    "09900000042, INVALID_PREFIX",
    "99000000043, CHECKSUM_MISMATCH",
    "99000000052, CHECKSUM_MISMATCH"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(Ykn.isValid(input)).isFalse();
    assertThat(Ykn.validate(input).invalidReason()).contains(reason);
  }

  @Test
  void normalizesAndMasks() {
    assertThat(Ykn.normalize("990 000 000 42")).isEqualTo("99000000042");
    assertThat(Ykn.normalize(null)).isNull();
    assertThat(Ykn.mask("990 000 000 42")).isEqualTo("990******42");
    assertThat(Ykn.mask("990")).isEqualTo("***");
    assertThat(Ykn.mask(null)).isNull();
  }

  @Test
  void ofAndParse() {
    Ykn ykn = Ykn.of("990-000-000-42");

    assertThat(ykn.value()).isEqualTo("99000000042");
    assertThat(ykn.masked()).isEqualTo("990******42");
    assertThat(ykn).hasToString("990******42");
    assertThat(Ykn.parse("99000000042")).contains(ykn);
    assertThat(Ykn.parse("10000000146")).isEmpty();
    assertThatThrownBy(() -> Ykn.of("10000000146"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid YKN: INVALID_PREFIX")
        .hasMessageNotContaining("10000000146");
  }

  @Test
  void equalsAndHashCode() {
    Ykn a = Ykn.of("99000000042");

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(Ykn.of("990 000 000 42"))
        .hasSameHashCodeAs(Ykn.of("990 000 000 42"))
        .isNotEqualTo(Ykn.of("99123456740"))
        .isNotEqualTo(Tckn.of("10000000146"))
        .isNotEqualTo(null);
  }

  @Test
  void randomIsValidAndNeverCitizen() {
    SplittableRandom rng = new SplittableRandom(42);
    for (int i = 0; i < 10_000; i++) {
      String value = Ykn.random(rng).value();
      assertThat(Ykn.isValid(value)).as(value).isTrue();
      assertThat(Tckn.isValid(value)).as(value).isFalse();
    }
    assertThat(Ykn.isValid(Ykn.random().value())).isTrue();
  }
}
