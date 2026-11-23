package io.github.ceydrew.trvalidation.jakarta;

import static io.github.ceydrew.trvalidation.jakarta.Validators.isValid;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ceydrew.trvalidation.CardNetwork;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConstraintsTest {

  /** One field per constraint and option. */
  static class Bean {
    @Tckn String tckn;

    @Tckn(allowYkn = true)
    String tcknOrYkn;

    @Ykn String ykn;
    @Vkn String vkn;
    @TrIban String iban;

    @TrIban(allowForeign = true)
    String anyIban;

    @TrPlate String plate;
    @TrPhone String phone;

    @TrPhone(mobileOnly = true)
    String mobile;

    @CardNumber String card;

    @CardNumber(networks = {CardNetwork.VISA, CardNetwork.TROY})
    String visaOrTroy;

    @TrPostalCode String postalCode;
  }

  @ParameterizedTest
  @CsvSource({
    "tckn, 10000000146, true",
    "tckn, 100 000 001 46, true",
    "tckn, 10000000147, false",
    "tckn, 99000000042, false",
    "tckn, '', false",
    "tcknOrYkn, 10000000146, true",
    "tcknOrYkn, 99000000042, true",
    "tcknOrYkn, 99000000043, false",
    "ykn, 99000000042, true",
    "ykn, 10000000146, false",
    "vkn, 1234567890, true",
    "vkn, 1234567891, false",
    "iban, TR33 0006 1005 1978 6457 8413 26, true",
    "iban, tr330006100519786457841326, true",
    "iban, DE89370400440532013000, false",
    "iban, TR340006100519786457841326, false",
    "anyIban, DE89370400440532013000, true",
    "anyIban, TR330006100519786457841326, true",
    "anyIban, DE89370400440532013001, false",
    "plate, 34 ABC 123, true",
    "plate, 34abc123, true",
    "plate, 82 A 1234, false",
    "phone, 0532 123 45 67, true",
    "phone, 0212 123 45 67, true",
    "phone, +1 202 555 0100, false",
    "mobile, +90 532 123 45 67, true",
    "mobile, 0212 123 45 67, false",
    "mobile, 0850 123 45 67, false",
    "mobile, 123, false",
    "card, 4111 1111 1111 1111, true",
    "card, 6011111111111117, true",
    "card, 4111111111111112, false",
    "visaOrTroy, 4111111111111111, true",
    "visaOrTroy, 5555555555554444, false",
    "visaOrTroy, 6011111111111117, false",
    "visaOrTroy, 4111111111111112, false",
    "postalCode, 34710, true",
    "postalCode, 99999, false"
  })
  void validatesThroughCore(String property, String value, boolean expected) {
    assertThat(isValid(Bean.class, property, value)).isEqualTo(expected);
  }

  @Test
  void troyCardMatchesNetworkFilter() {
    String troy =
        io.github.ceydrew.trvalidation.CardNumber.random(CardNetwork.TROY, new SplittableRandom(1))
            .value();

    assertThat(isValid(Bean.class, "visaOrTroy", troy)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "tckn",
        "tcknOrYkn",
        "ykn",
        "vkn",
        "iban",
        "anyIban",
        "plate",
        "phone",
        "mobile",
        "card",
        "visaOrTroy",
        "postalCode"
      })
  void nullIsValid(String property) {
    assertThat(isValid(Bean.class, property, null)).isTrue();
  }

  static class Builders {
    @Tckn StringBuilder tckn;
  }

  @Test
  void acceptsAnyCharSequence() {
    assertThat(isValid(Builders.class, "tckn", new StringBuilder("10000000146"))).isTrue();
    assertThat(isValid(Builders.class, "tckn", new StringBuilder("10000000147"))).isFalse();
  }

  record Customer(@Tckn String tckn, List<@TrPhone String> phones) {}

  @Test
  void worksOnRecordsAndContainerElements() {
    Set<ConstraintViolation<Customer>> violations =
        Validators.english()
            .validate(new Customer("10000000147", List.of("0532 123 45 67", "123")));

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .containsExactlyInAnyOrder("tckn", "phones[1].<list element>");
  }

  interface Strict {}

  static class Grouped {
    @Tckn(groups = Strict.class)
    String tckn = "10000000147";
  }

  @Test
  void respectsGroups() {
    assertThat(Validators.english().validate(new Grouped())).isEmpty();
    assertThat(Validators.english().validate(new Grouped(), Strict.class)).hasSize(1);
  }
}
