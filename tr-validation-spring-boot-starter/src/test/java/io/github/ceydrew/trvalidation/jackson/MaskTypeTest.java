package io.github.ceydrew.trvalidation.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class MaskTypeTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "TCKN | 10000000146 | 100******46",
        "YKN | 99000000042 | 990******42",
        "VKN | 1234567890 | 12******90",
        "IBAN | TR330006100519786457841326 | TR33 **** **** **** **** **13 26",
        "CARD_NUMBER | 4111111111111111 | **** **** **** 1111",
        "PHONE_NUMBER | 05321234567 | +90 532 *** ** 67",
        "LICENSE_PLATE | 34ABC123 | 34 *** *23",
        "POSTAL_CODE | 34710 | 34***",
        "FULL | Ayşe Yılmaz | ***********"
      })
  void masks(MaskType type, String value, String expected) {
    assertThat(type.apply(value)).isEqualTo(expected);
  }

  @ParameterizedTest
  @EnumSource(MaskType.class)
  void keepsNullAndHidesMalformedInput(MaskType type) {
    assertThat(type.apply(null)).isNull();
    assertThat(type.apply("12a")).doesNotContainPattern("[0-9a]");
  }
}
