package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IbanTest {

  private static final String TR = "TR330006100519786457841326";

  @ParameterizedTest
  @ValueSource(
      strings = {
        TR,
        "TR33 0006 1005 1978 6457 8413 26",
        "tr33 0006 1005 1978 6457 8413 26",
        "GB82WEST12345698765432",
        "GB82 WEST 1234 5698 7654 32",
        "DE89370400440532013000",
        "NO9386011117947",
        "BE68539007547034",
        "TN5910006035183598478831"
      })
  void acceptsValidIbans(String input) {
    assertThat(Iban.isValid(input)).isTrue();
    assertThat(Iban.validate(input)).isSameAs(ValidationResult.valid());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", " - "})
  void rejectsEmptyInput(String input) {
    assertThat(Iban.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
    assertThat(Iban.validateTr(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "TR33_0006100519786457841326, INVALID_CHARACTER",
    "TR33 0006 1005 1978 6457 8413 2İ, INVALID_CHARACTER",
    "1R330006100519786457841326, INVALID_COUNTRY_CODE",
    "T1330006100519786457841326, INVALID_COUNTRY_CODE",
    "XX330006100519786457841326, INVALID_COUNTRY_CODE",
    "US330006100519786457841326, INVALID_COUNTRY_CODE",
    "TRA30006100519786457841326, INVALID_FORMAT",
    "TR3A0006100519786457841326, INVALID_FORMAT",
    "T, WRONG_LENGTH",
    "TR, WRONG_LENGTH",
    "TR33000610051978645784132, WRONG_LENGTH",
    "TR3300061005197864578413261, WRONG_LENGTH",
    "TR340006100519786457841326, CHECKSUM_MISMATCH",
    "TR330006100519786457841327, CHECKSUM_MISMATCH",
    "GB82WEST12345698765433, CHECKSUM_MISMATCH"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(Iban.isValid(input)).isFalse();
    assertThat(Iban.validate(input).invalidReason()).contains(reason);
  }

  @Test
  void matchesBigIntegerReferenceForAllCheckDigits() {
    String bban = TR.substring(4);
    for (int check = 0; check < 100; check++) {
      String candidate = "TR" + (check < 10 ? "0" : "") + check + bban;
      assertThat(Iban.isValid(candidate)).as(candidate).isEqualTo(referenceIsValid(candidate));
    }
  }

  @Test
  void validateTrRequiresTurkishStructure() {
    assertThat(Iban.validateTr(TR)).isSameAs(ValidationResult.valid());
    assertThat(Iban.isValidTr("tr33 0006 1005 1978 6457 8413 26")).isTrue();
    assertThat(Iban.validateTr("DE89370400440532013000").invalidReason())
        .contains(InvalidReason.INVALID_COUNTRY_CODE);
    assertThat(Iban.validateTr("TN5910006035183598478831").invalidReason())
        .contains(InvalidReason.INVALID_COUNTRY_CODE);
    assertThat(Iban.validateTr("TR340006100519786457841326").invalidReason())
        .contains(InvalidReason.CHECKSUM_MISMATCH);

    String letterInBankCode = withCheckDigits("TR", "0A06100519786457841326");
    String letterInReserved = withCheckDigits("TR", "00061A0519786457841326");
    String letterInAccount = withCheckDigits("TR", "000610A519786457841326");
    assertThat(Iban.validateTr(letterInAccount)).isSameAs(ValidationResult.valid());
    assertThat(Iban.isValid(letterInBankCode)).isTrue();
    assertThat(Iban.validateTr(letterInBankCode).invalidReason())
        .contains(InvalidReason.INVALID_FORMAT);
    assertThat(Iban.validateTr(letterInReserved).invalidReason())
        .contains(InvalidReason.INVALID_FORMAT);
    assertThat(Iban.validateTr(withCheckDigits("TR", "0006100519786457841326AB")))
        .isSameAs(ValidationResult.invalid(InvalidReason.WRONG_LENGTH));
  }

  @Test
  void normalizesFormatsAndMasks() {
    assertThat(Iban.normalize("tr33 0006-1005")).isEqualTo("TR3300061005");
    assertThat(Iban.normalize(null)).isNull();
    assertThat(Iban.format("tr330006100519786457841326"))
        .isEqualTo("TR33 0006 1005 1978 6457 8413 26");
    assertThat(Iban.format("TR33")).isNull();
    assertThat(Iban.format(null)).isNull();
    assertThat(Iban.mask(TR)).isEqualTo("TR33 **** **** **** **** **13 26");
    assertThat(Iban.mask("NO93 8601 1117 947")).isEqualTo("NO93 **** ***7 947");
    assertThat(Iban.mask("TR12")).isEqualTo("****");
    assertThat(Iban.mask("TR12 3456 7890 1234 5678 9012 3456 7890 123"))
        .doesNotContainPattern("\\d");
    assertThat(Iban.mask("")).isEmpty();
    assertThat(Iban.mask(null)).isNull();
  }

  @Test
  void ofAndInstanceAccessors() {
    Iban iban = Iban.of("tr33 0006 1005 1978 6457 8413 26");

    assertThat(iban.value()).isEqualTo(TR);
    assertThat(iban.countryCode()).isEqualTo("TR");
    assertThat(iban.bankCode()).contains("00061");
    assertThat(iban.formatted()).isEqualTo("TR33 0006 1005 1978 6457 8413 26");
    assertThat(iban.masked()).isEqualTo("TR33 **** **** **** **** **13 26");
    assertThat(iban).hasToString("TR33 **** **** **** **** **13 26");

    Iban german = Iban.of("DE89370400440532013000");
    assertThat(german.countryCode()).isEqualTo("DE");
    assertThat(german.bankCode()).isEmpty();
  }

  @Test
  void ofRejectsInvalidInputWithoutLeakingIt() {
    assertThatThrownBy(() -> Iban.of("TR340006100519786457841326"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid IBAN: CHECKSUM_MISMATCH")
        .hasMessageNotContaining("0006100519786457841326");
  }

  @Test
  void parseAndEquality() {
    Iban a = Iban.of(TR);

    assertThat(Iban.parse("TR33 0006 1005 1978 6457 8413 26")).contains(a);
    assertThat(Iban.parse("TR00")).isEmpty();
    assertThat(a)
        .isEqualTo(a)
        .hasSameHashCodeAs(Iban.of(TR.toLowerCase(java.util.Locale.ROOT)))
        .isNotEqualTo(Iban.of("DE89370400440532013000"))
        .isNotEqualTo(TR)
        .isNotEqualTo(null);
  }

  @Test
  void randomTrIsValidTurkishIban() {
    SplittableRandom rng = new SplittableRandom(42);
    for (int i = 0; i < 10_000; i++) {
      Iban iban = Iban.randomTr(rng);
      assertThat(Iban.validateTr(iban.value())).as(iban.value()).isSameAs(ValidationResult.valid());
      assertThat(referenceIsValid(iban.value())).isTrue();
      assertThat(iban.value().charAt(9)).isEqualTo('0');
    }
    assertThat(Iban.isValidTr(Iban.randomTr().value())).isTrue();
  }

  /** ISO 13616 mod-97 with BigInteger, independent of {@link Iban}. */
  static boolean referenceIsValid(String iban) {
    return numeric(iban.substring(4) + iban.substring(0, 4))
        .mod(BigInteger.valueOf(97))
        .equals(BigInteger.ONE);
  }

  static String withCheckDigits(String country, String bban) {
    int check = 98 - numeric(bban + country + "00").mod(BigInteger.valueOf(97)).intValue();
    return country + (check < 10 ? "0" : "") + check + bban;
  }

  private static BigInteger numeric(String s) {
    StringBuilder digits = new StringBuilder();
    for (char c : s.toCharArray()) {
      digits.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(c - 'A' + 10));
    }
    return new BigInteger(digits.toString());
  }
}
