package io.github.ceydrew.trvalidation.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ceydrew.trvalidation.CardNumber;
import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PostalCode;
import io.github.ceydrew.trvalidation.Tckn;
import io.github.ceydrew.trvalidation.Vkn;
import io.github.ceydrew.trvalidation.Ykn;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Behavior shared by the Jackson 2 and Jackson 3 modules. */
abstract class JacksonModuleContract {

  /** Serializes with the module registered. */
  abstract String write(Object value, boolean masking);

  /** Deserializes with the module registered; wraps checked exceptions. */
  abstract <T> T read(String json, Class<T> type);

  record Customer(
      @Masked(MaskType.TCKN) String tckn,
      @Masked(MaskType.PHONE_NUMBER) String gsm,
      @Masked(MaskType.FULL) String motherName,
      String name) {}

  static class Bean {
    private final String iban = "TR330006100519786457841326";

    @Masked(MaskType.IBAN)
    public String getIban() {
      return iban;
    }

    @Masked(MaskType.CARD_NUMBER)
    public StringBuilder card = new StringBuilder("4111111111111111");
  }

  record Invalid(@Masked(MaskType.TCKN) List<String> tckns) {}

  record Values(
      Tckn tckn,
      Ykn ykn,
      Vkn vkn,
      Iban iban,
      CardNumber card,
      PhoneNumber phone,
      LicensePlate plate,
      PostalCode postalCode) {}

  static final Values VALUES =
      new Values(
          Tckn.of("10000000146"),
          Ykn.of("99000000042"),
          Vkn.of("1234567890"),
          Iban.of("TR330006100519786457841326"),
          CardNumber.of("4111111111111111"),
          PhoneNumber.of("05321234567"),
          LicensePlate.of("34ABC123"),
          PostalCode.of("34710"));

  @Test
  void masksAnnotatedRecordComponents() {
    assertThat(write(new Customer("10000000146", "05321234567", "Ayşe", "Ali"), true))
        .isEqualTo(
            "{\"tckn\":\"100******46\",\"gsm\":\"+90 532 *** ** 67\","
                + "\"motherName\":\"****\",\"name\":\"Ali\"}");
  }

  @Test
  void masksAnnotatedGettersAndCharSequenceFields() {
    assertThat(write(new Bean(), true))
        .contains("\"iban\":\"TR33 **** **** **** **** **13 26\"")
        .contains("\"card\":\"**** **** **** 1111\"");
  }

  @Test
  void leavesNullsAlone() {
    assertThat(write(new Customer(null, null, null, null), true))
        .isEqualTo("{\"tckn\":null,\"gsm\":null,\"motherName\":null,\"name\":null}");
  }

  @Test
  void writesFullValuesWhenMaskingIsDisabled() {
    assertThat(write(new Customer("10000000146", "05321234567", "Ayşe", "Ali"), false))
        .isEqualTo(
            "{\"tckn\":\"10000000146\",\"gsm\":\"05321234567\","
                + "\"motherName\":\"Ayşe\",\"name\":\"Ali\"}");
  }

  @Test
  void rejectsMaskedOnNonCharSequence() {
    assertThatThrownBy(() -> write(new Invalid(List.of("10000000146")), true))
        .hasStackTraceContaining("@Masked is only supported on CharSequence properties")
        .hasStackTraceContaining("tckns");
  }

  @Test
  void writesValueTypesMasked() {
    assertThat(write(VALUES, true))
        .isEqualTo(
            "{\"tckn\":\"100******46\",\"ykn\":\"990******42\",\"vkn\":\"12******90\","
                + "\"iban\":\"TR33 **** **** **** **** **13 26\","
                + "\"card\":\"**** **** **** 1111\",\"phone\":\"+90 532 *** ** 67\","
                + "\"plate\":\"34 *** *23\",\"postalCode\":\"34***\"}");
  }

  @Test
  void writesValueTypesInFullWhenMaskingIsDisabled() {
    assertThat(write(VALUES, false))
        .isEqualTo(
            "{\"tckn\":\"10000000146\",\"ykn\":\"99000000042\",\"vkn\":\"1234567890\","
                + "\"iban\":\"TR330006100519786457841326\",\"card\":\"4111111111111111\","
                + "\"phone\":\"+905321234567\",\"plate\":\"34ABC123\",\"postalCode\":\"34710\"}");
  }

  @Test
  void readsValueTypesFromStrings() {
    String json = write(VALUES, false);

    assertThat(read(json, Values.class)).isEqualTo(VALUES);
    assertThat(read("\"100 000 001 46\"", Tckn.class)).isEqualTo(Tckn.of("10000000146"));
    assertThat(read("null", Tckn.class)).isNull();
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "\"10000000147\" | Invalid TCKN: CHECKSUM_MISMATCH | 10000000147",
        "\"TR340006100519786457841326\" | Invalid IBAN: CHECKSUM_MISMATCH | 0006100519786457841326",
        "\"0532 123 45\" | Invalid phone number: WRONG_LENGTH | 0532 123 45"
      })
  void rejectsInvalidValuesWithoutEchoingThem(String json, String message, String secret) {
    Class<?> type =
        json.startsWith("\"TR")
            ? Iban.class
            : json.startsWith("\"0") ? PhoneNumber.class : Tckn.class;

    assertThatThrownBy(() -> read(json, type))
        .hasMessageContaining(message)
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain(secret));
  }

  @Test
  void rejectsNonStringTokens() {
    assertThatThrownBy(() -> read("10000000146", Tckn.class))
        .hasMessageContaining("Expected a JSON string for Tckn");
  }
}
