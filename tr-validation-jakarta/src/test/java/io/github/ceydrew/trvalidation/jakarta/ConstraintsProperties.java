package io.github.ceydrew.trvalidation.jakarta;

import static io.github.ceydrew.trvalidation.jakarta.Validators.isValid;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PostalCode;
import java.util.Random;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/** The constraints agree with the core validators on generated and arbitrary input. */
class ConstraintsProperties {

  static class Bean {
    @Tckn String tckn;

    @Tckn(allowYkn = true)
    String tcknOrYkn;

    @Ykn String ykn;
    @Vkn String vkn;
    @TrIban String iban;
    @TrPlate String plate;
    @TrPhone String phone;
    @CardNumber String card;
    @TrPostalCode String postalCode;
  }

  @Property(tries = 200)
  void generatedValuesAreValid(@ForAll Random random) {
    assertThat(
            isValid(Bean.class, "tckn", io.github.ceydrew.trvalidation.Tckn.random(random).value()))
        .isTrue();
    assertThat(
            isValid(
                Bean.class, "tcknOrYkn", io.github.ceydrew.trvalidation.Ykn.random(random).value()))
        .isTrue();
    assertThat(
            isValid(Bean.class, "ykn", io.github.ceydrew.trvalidation.Ykn.random(random).value()))
        .isTrue();
    assertThat(
            isValid(Bean.class, "vkn", io.github.ceydrew.trvalidation.Vkn.random(random).value()))
        .isTrue();
    assertThat(isValid(Bean.class, "iban", Iban.randomTr(random).formatted())).isTrue();
    assertThat(isValid(Bean.class, "plate", LicensePlate.random(random).formatted())).isTrue();
    assertThat(isValid(Bean.class, "postalCode", PostalCode.random(random).value())).isTrue();
  }

  @Property(tries = 300)
  void agreesWithCoreOnArbitraryInput(@ForAll String input) {
    assertThat(isValid(Bean.class, "tckn", input))
        .isEqualTo(io.github.ceydrew.trvalidation.Tckn.isValid(input));
    assertThat(isValid(Bean.class, "vkn", input))
        .isEqualTo(io.github.ceydrew.trvalidation.Vkn.isValid(input));
    assertThat(isValid(Bean.class, "iban", input)).isEqualTo(Iban.isValidTr(input));
    assertThat(isValid(Bean.class, "plate", input)).isEqualTo(LicensePlate.isValid(input));
    assertThat(isValid(Bean.class, "phone", input))
        .isEqualTo(io.github.ceydrew.trvalidation.PhoneNumber.isValid(input));
    assertThat(isValid(Bean.class, "card", input))
        .isEqualTo(io.github.ceydrew.trvalidation.CardNumber.isValid(input));
  }
}
