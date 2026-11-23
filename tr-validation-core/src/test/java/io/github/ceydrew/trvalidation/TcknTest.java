package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TcknTest {

  private static final char NBSP = (char) 0xA0;
  private static final char ARABIC_INDIC_ONE = (char) 0x0661;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "10000000146",
        "11111111110",
        "12345678950",
        // (odd * 7 - even) is negative here, so the 10th digit needs floorMod
        "19090909018",
        "98765432150"
      })
  void acceptsValidNumbers(String input) {
    assertThat(Tckn.isValid(input)).isTrue();
    assertThat(Tckn.validate(input)).isSameAs(ValidationResult.valid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "100 000 001 46",
        "100-000-001-46",
        " 10000000146 ",
        "\t10000000146\n",
        "10000000146\r\n",
        "100" + NBSP + "000" + NBSP + "00146"
      })
  void ignoresSeparators(String input) {
    assertThat(Tckn.isValid(input)).isTrue();
    assertThat(Tckn.normalize(input)).isEqualTo("10000000146");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "-", " - \t"})
  void rejectsEmptyInput(String input) {
    assertThat(Tckn.isValid(input)).isFalse();
    assertThat(Tckn.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "1000000014a, INVALID_CHARACTER",
    "+10000000146, INVALID_CHARACTER",
    // Arabic-Indic digit one; Character.isDigit would accept it
    ARABIC_INDIC_ONE + "000000014, INVALID_CHARACTER",
    "123456789012345x, INVALID_CHARACTER",
    "1, WRONG_LENGTH",
    "1000000014, WRONG_LENGTH",
    "100000001460, WRONG_LENGTH",
    "1234567890123456, WRONG_LENGTH",
    "01000000146, INVALID_PREFIX",
    "00000000000, INVALID_PREFIX",
    "99000000042, INVALID_PREFIX",
    "10000000156, CHECKSUM_MISMATCH",
    "10000000147, CHECKSUM_MISMATCH",
    "20000000146, CHECKSUM_MISMATCH",
    "91000000146, CHECKSUM_MISMATCH"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(Tckn.isValid(input)).isFalse();
    assertThat(Tckn.validate(input).invalidReason()).contains(reason);
  }

  @Test
  void normalizeKeepsNullAndInvalidCharacters() {
    assertThat(Tckn.normalize(null)).isNull();
    assertThat(Tckn.normalize("a b-c")).isEqualTo("abc");
  }

  @ParameterizedTest
  @CsvSource({
    "10000000146, 100******46",
    "'100 000 001 46', 100******46",
    // checksum is not required for masking, only the length
    "10000000147, 100******47",
    "12345, *****",
    "100000001460, ************",
    "'', ''"
  })
  void masks(String input, String expected) {
    assertThat(Tckn.mask(input)).isEqualTo(expected);
  }

  @Test
  void maskOfNullIsNull() {
    assertThat(Tckn.mask(null)).isNull();
  }

  @Test
  void ofReturnsNormalizedValue() {
    Tckn tckn = Tckn.of(" 100 000 001 46 ");

    assertThat(tckn.value()).isEqualTo("10000000146");
    assertThat(tckn.masked()).isEqualTo("100******46");
    assertThat(tckn).hasToString("100******46");
  }

  @Test
  void ofRejectsInvalidInputWithoutLeakingIt() {
    assertThatThrownBy(() -> Tckn.of("10000000147"))
        .isInstanceOf(InvalidValueException.class)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid TCKN: CHECKSUM_MISMATCH")
        .hasMessageNotContaining("10000000147")
        .satisfies(
            e -> {
              InvalidValueException ex = (InvalidValueException) e;
              assertThat(ex.type()).isEqualTo("TCKN");
              assertThat(ex.reason()).isEqualTo(InvalidReason.CHECKSUM_MISMATCH);
            });
    assertThatThrownBy(() -> Tckn.of(null))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid TCKN: EMPTY");
  }

  @Test
  void parse() {
    assertThat(Tckn.parse("100-000-001-46")).map(Tckn::value).contains("10000000146");
    assertThat(Tckn.parse("10000000147")).isEmpty();
    assertThat(Tckn.parse(null)).isEmpty();
  }

  @Test
  void equalsAndHashCodeUseNormalizedValue() {
    Tckn a = Tckn.of("10000000146");
    Tckn b = Tckn.of("100 000 001 46");
    Tckn c = Tckn.of("11111111110");

    assertThat(a).isEqualTo(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
    assertThat(a).isNotEqualTo("10000000146").isNotEqualTo(null);
  }

  @Test
  void randomIsValidAndNeverForeign() {
    SplittableRandom rng = new SplittableRandom(42);
    for (int i = 0; i < 10_000; i++) {
      String value = Tckn.random(rng).value();
      assertThat(Tckn.isValid(value)).as(value).isTrue();
      assertThat(Ykn.isValid(value)).as(value).isFalse();
    }
    assertThat(Tckn.isValid(Tckn.random().value())).isTrue();
  }

  @Test
  void randomIsReproducibleWithSeed() {
    assertThat(Tckn.random(new SplittableRandom(7)))
        .isEqualTo(Tckn.random(new SplittableRandom(7)));
  }
}
