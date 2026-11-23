package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CardNumberTest {

  @ParameterizedTest
  @CsvSource({
    "4111111111111111, VISA",
    "4012 8888 8888 1881, VISA",
    "4222222222222, VISA",
    "5555-5555-5555-4444, MASTERCARD",
    "5105105105105100, MASTERCARD",
    "2223003122003222, MASTERCARD",
    "378282246310005, AMEX",
    "371449635398431, AMEX",
    "6011111111111117, UNKNOWN",
    "3530111333300000, UNKNOWN"
  })
  void acceptsValidNumbersAndDetectsNetwork(String input, CardNetwork network) {
    assertThat(CardNumber.isValid(input)).isTrue();
    assertThat(CardNumber.validate(input)).isSameAs(ValidationResult.valid());
    assertThat(CardNumber.network(input)).contains(network);
  }

  @ParameterizedTest
  @CsvSource({
    // prefix and length combinations around the detection rules
    "41111111111111, UNKNOWN",
    "4111111111111111111, VISA",
    "3411111111111111, UNKNOWN",
    "3511111111111111, UNKNOWN",
    "5011111111111111, UNKNOWN",
    "5611111111111111, UNKNOWN",
    "5111111111111111111, UNKNOWN",
    "2220111111111111, UNKNOWN",
    "2221111111111111, MASTERCARD",
    "2720111111111111, MASTERCARD",
    "2721111111111111, UNKNOWN",
    "9792111111111111, TROY",
    "9792111111111111111, UNKNOWN",
    "9793111111111111, UNKNOWN"
  })
  void detectsNetworkByPrefixAndLength(String sample, CardNetwork network) {
    // replace the sample's last digit with a correct Luhn check digit, keeping prefix and length
    String number = withLuhnCheckDigit(sample.substring(0, sample.length() - 1));

    assertThat(CardNumber.network(number)).as(number).contains(network);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "- -"})
  void rejectsEmptyInput(String input) {
    assertThat(CardNumber.validate(input).invalidReason()).contains(InvalidReason.EMPTY);
  }

  @ParameterizedTest
  @CsvSource({
    "4111 1111 1111 111x, INVALID_CHARACTER",
    "4111/1111/1111/1111, INVALID_CHARACTER",
    "41111111111, WRONG_LENGTH",
    "41111111111111111111, WRONG_LENGTH",
    "4111111111111112, CHECKSUM_MISMATCH",
    "378282246310006, CHECKSUM_MISMATCH"
  })
  void explainsInvalidInput(String input, InvalidReason reason) {
    assertThat(CardNumber.isValid(input)).isFalse();
    assertThat(CardNumber.validate(input).invalidReason()).contains(reason);
    assertThat(CardNumber.network(input)).isEmpty();
  }

  @Test
  void formatsByNetwork() {
    assertThat(CardNumber.format("4111111111111111")).isEqualTo("4111 1111 1111 1111");
    assertThat(CardNumber.format("378282246310005")).isEqualTo("3782 822463 10005");
    assertThat(CardNumber.format("4222222222222")).isEqualTo("4222 2222 2222 2");
    assertThat(CardNumber.format("123")).isNull();
    assertThat(CardNumber.format(null)).isNull();
  }

  @Test
  void masksKeepingOnlyLastFour() {
    assertThat(CardNumber.mask("4111 1111 1111 1111")).isEqualTo("**** **** **** 1111");
    assertThat(CardNumber.mask("378282246310005")).isEqualTo("**** ****** *0005");
    // invalid checksum: still masked safely, grouped in fours
    assertThat(CardNumber.mask("378282246310006")).isEqualTo("**** **** ***0 006");
    assertThat(CardNumber.mask("1234")).isEqualTo("****");
    assertThat(CardNumber.mask("12345678901234567890")).isEqualTo("********************");
    assertThat(CardNumber.mask(null)).isNull();
  }

  @Test
  void normalize() {
    assertThat(CardNumber.normalize("4111-1111 1111 1111")).isEqualTo("4111111111111111");
    assertThat(CardNumber.normalize(null)).isNull();
  }

  @Test
  void ofParseAndAccessors() {
    CardNumber card = CardNumber.of("4111 1111 1111 1111");

    assertThat(card.value()).isEqualTo("4111111111111111");
    assertThat(card.network()).isEqualTo(CardNetwork.VISA);
    assertThat(card.formatted()).isEqualTo("4111 1111 1111 1111");
    assertThat(card.masked()).isEqualTo("**** **** **** 1111");
    assertThat(card).hasToString("**** **** **** 1111");
    assertThat(CardNumber.parse("4111111111111111")).contains(card);
    assertThat(CardNumber.parse("4111111111111112")).isEmpty();
    assertThat(card)
        .isEqualTo(card)
        .hasSameHashCodeAs(CardNumber.of("4111111111111111"))
        .isNotEqualTo(CardNumber.of("5555555555554444"))
        .isNotEqualTo("4111111111111111")
        .isNotEqualTo(null);
    assertThatThrownBy(() -> CardNumber.of("4111111111111112"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessage("Invalid card number: CHECKSUM_MISMATCH")
        .hasMessageNotContaining("4111111111111112");
  }

  @ParameterizedTest
  @EnumSource(value = CardNetwork.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
  void randomProducesValidNumbersOfTheNetwork(CardNetwork network) {
    SplittableRandom rng = new SplittableRandom(42);
    for (int i = 0; i < 2_000; i++) {
      CardNumber card = CardNumber.random(network, rng);
      assertThat(CardNumber.isValid(card.value())).as(card.value()).isTrue();
      assertThat(card.network()).isEqualTo(network);
    }
  }

  @Test
  void randomWithoutNetworkIsValidAndKnown() {
    for (int i = 0; i < 100; i++) {
      CardNumber card = CardNumber.random();
      assertThat(CardNumber.isValid(card.value())).isTrue();
      assertThat(card.network()).isNotEqualTo(CardNetwork.UNKNOWN);
    }
  }

  @Test
  void randomRejectsUnknownAndNull() {
    SplittableRandom rng = new SplittableRandom(1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CardNumber.random(CardNetwork.UNKNOWN, rng));
    assertThatNullPointerException().isThrownBy(() -> CardNumber.random(null, rng));
  }

  /** Appends the Luhn check digit, independent of {@link CardNumber}. */
  static String withLuhnCheckDigit(String payload) {
    int sum = 0;
    boolean doubleIt = true;
    for (int i = payload.length() - 1; i >= 0; i--) {
      int d = payload.charAt(i) - '0';
      if (doubleIt) {
        d *= 2;
        if (d > 9) {
          d -= 9;
        }
      }
      sum += d;
      doubleIt = !doubleIt;
    }
    return payload + (10 - sum % 10) % 10;
  }
}
