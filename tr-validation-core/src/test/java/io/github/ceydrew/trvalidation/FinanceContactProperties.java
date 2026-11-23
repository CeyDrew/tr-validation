package io.github.ceydrew.trvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;

/** Property-based tests for IBAN, card number, phone number, license plate and postal code. */
class FinanceContactProperties {

  @Property
  void randomIbanIsValidAndMatchesReference(@ForAll Random random) {
    String iban = Iban.randomTr(random).value();

    assertThat(Iban.isValidTr(iban)).isTrue();
    assertThat(IbanTest.referenceIsValid(iban)).isTrue();
  }

  @Property
  void changingOneIbanDigitInvalidates(
      @ForAll Random random,
      @ForAll @IntRange(min = 2, max = 25) int index,
      @ForAll @IntRange(min = 1, max = 9) int delta) {
    char[] chars = Iban.randomTr(random).value().toCharArray();
    chars[index] = (char) ('0' + (chars[index] - '0' + delta) % 10);

    // mod 97 detects every single-character substitution
    assertThat(Iban.isValid(new String(chars))).isFalse();
  }

  @Property
  void ibanAgreesWithReferenceOnArbitraryDigits(
      @ForAll @NumericChars @StringLength(24) String digits) {
    String candidate = "TR" + digits;

    assertThat(Iban.isValid(candidate)).isEqualTo(IbanTest.referenceIsValid(candidate));
  }

  @Property
  void randomCardIsValid(@ForAll Random random) {
    for (CardNetwork network :
        new CardNetwork[] {
          CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.TROY
        }) {
      CardNumber card = CardNumber.random(network, random);
      assertThat(CardNumber.isValid(card.value())).isTrue();
      assertThat(CardNumber.network(card.formatted())).contains(network);
    }
  }

  @Property
  void changingOneCardDigitInvalidates(
      @ForAll Random random,
      @ForAll @IntRange(min = 0, max = 15) int index,
      @ForAll @IntRange(min = 1, max = 9) int delta) {
    char[] chars = CardNumber.random(CardNetwork.VISA, random).value().toCharArray();
    chars[index] = (char) ('0' + (chars[index] - '0' + delta) % 10);

    // Luhn detects every single-digit substitution
    assertThat(CardNumber.isValid(new String(chars))).isFalse();
  }

  @Property
  void cardAgreesWithReferenceLuhn(
      @ForAll @NumericChars @StringLength(min = 11, max = 18) String payload) {
    String valid = CardNumberTest.withLuhnCheckDigit(payload);

    assertThat(CardNumber.isValid(valid)).isTrue();
  }

  @Property
  void phoneFormsNormalizeToTheSameE164(@ForAll @NumericChars @StringLength(9) String subscriber) {
    String national = "5" + subscriber;
    String e164 = "+90" + national;

    assertThat(PhoneNumber.normalize(national)).isEqualTo(e164);
    assertThat(PhoneNumber.normalize("0" + national)).isEqualTo(e164);
    assertThat(PhoneNumber.normalize("+90 " + national)).isEqualTo(e164);
    assertThat(PhoneNumber.normalize("0090" + national)).isEqualTo(e164);
    assertThat(PhoneNumber.normalize(PhoneNumber.format(national))).isEqualTo(e164);
  }

  @Property
  void randomPlateRoundTripsThroughFormat(@ForAll Random random) {
    LicensePlate plate = LicensePlate.random(random);

    assertThat(LicensePlate.of(plate.formatted())).isEqualTo(plate);
    assertThat(LicensePlate.mask(plate.value())).startsWith(plate.value().substring(0, 2));
  }

  @Property
  void randomPostalCodeIsValid(@ForAll Random random) {
    PostalCode code = PostalCode.random(random);

    assertThat(PostalCode.isValid(code.value())).isTrue();
    assertThat(code.provinceCode()).isBetween(1, 81);
  }

  @Property
  void arbitraryStringsNeverThrow(@ForAll String input) {
    assertThat(Iban.validate(input)).isNotNull();
    assertThat(Iban.validateTr(input)).isNotNull();
    assertThat(CardNumber.validate(input)).isNotNull();
    assertThat(PhoneNumber.validate(input)).isNotNull();
    assertThat(LicensePlate.validate(input)).isNotNull();
    assertThat(PostalCode.validate(input)).isNotNull();
    assertThat(Iban.mask(input)).isNotNull();
    assertThat(CardNumber.mask(input)).isNotNull();
    assertThat(PhoneNumber.mask(input)).isNotNull();
    assertThat(LicensePlate.mask(input)).isNotNull();
  }
}
