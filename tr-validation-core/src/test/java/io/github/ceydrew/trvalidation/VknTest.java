package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VknTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1234567890",
        // leading zero is allowed; also exercises the "t != 0 && v == 0 -> 9" rule at i = 0
        "0000000001",
        "123 456 7890",
        "123-456-7890"
      })
  void acceptsValidNumbers(String input) {
    assertThat(Vkn.isValid(input)).isTrue();
    assertThat(Vkn.validate(input)).isSameAs(ValidationResult.valid());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "--"})
  void rejectsEmptyInput(String input) {
    assertThat(Vkn.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "12345678A0, INVALID_CHARACTER",
    "12345678901x, INVALID_CHARACTER",
    "123456789, WRONG_LENGTH",
    "12345678901, WRONG_LENGTH",
    "1234567891, CHECKSUM_MISMATCH",
    "0000000000, CHECKSUM_MISMATCH"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(Vkn.isValid(input)).isFalse();
    assertThat(Vkn.validate(input).invalidReason()).contains(reason);
  }

  @Test
  void matchesReferenceImplementation() {
    SplittableRandom rng = new SplittableRandom(1);
    for (int i = 0; i < 10_000; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < 10; j++) {
        sb.append((char) ('0' + rng.nextInt(10)));
      }
      String candidate = sb.toString();
      assertThat(Vkn.isValid(candidate)).as(candidate).isEqualTo(referenceIsValid(candidate));
    }
  }

  @Test
  void normalizesAndMasks() {
    assertThat(Vkn.normalize("123-456-7890")).isEqualTo("1234567890");
    assertThat(Vkn.normalize(null)).isNull();
    assertThat(Vkn.mask("123 456 7890")).isEqualTo("12******90");
    assertThat(Vkn.mask("12345678901")).isEqualTo("***********");
    assertThat(Vkn.mask(null)).isNull();
  }

  @Test
  void ofAndParse() {
    Vkn vkn = Vkn.of("123 456 7890");

    assertThat(vkn.value()).isEqualTo("1234567890");
    assertThat(vkn.masked()).isEqualTo("12******90");
    assertThat(vkn).hasToString("12******90");
    assertThat(Vkn.parse("1234567890")).contains(vkn);
    assertThat(Vkn.parse("")).isEmpty();
    assertThatThrownBy(() -> Vkn.of("1234567891"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid VKN: CHECKSUM_MISMATCH")
        .hasMessageNotContaining("1234567891");
  }

  @Test
  void equalsAndHashCode() {
    Vkn a = Vkn.of("1234567890");

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(Vkn.of("123-456-7890"))
        .hasSameHashCodeAs(Vkn.of("123-456-7890"))
        .isNotEqualTo(Vkn.of("0000000001"))
        .isNotEqualTo("1234567890")
        .isNotEqualTo(null);
  }

  @Test
  void randomIsValid() {
    SplittableRandom rng = new SplittableRandom(42);
    for (int i = 0; i < 10_000; i++) {
      String value = Vkn.random(rng).value();
      assertThat(Vkn.isValid(value)).as(value).isTrue();
    }
    assertThat(Vkn.isValid(Vkn.random().value())).isTrue();
  }

  /** Straightforward transcription of the published algorithm, independent of {@link Vkn}. */
  private static boolean referenceIsValid(String vkn) {
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      int digit = vkn.charAt(i) - '0';
      int tmp = (digit + 9 - i) % 10;
      int v = (int) ((tmp * Math.pow(2, 9 - i)) % 9);
      if (tmp != 0 && v == 0) {
        v = 9;
      }
      sum += v;
    }
    int check = (10 - (sum % 10)) % 10;
    return check == vkn.charAt(9) - '0';
  }
}
