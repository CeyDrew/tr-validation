package io.github.ceydrew.trvalidation.jakarta;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MessagesTest {

  /** One invalid value per constraint. */
  record Invalid(
      @Tckn String tckn,
      @Ykn String ykn,
      @Vkn String vkn,
      @TrIban String iban,
      @TrPlate String plate,
      @TrPhone String phone,
      @CardNumber String card,
      @TrPostalCode String postalCode) {

    static final Invalid INSTANCE = new Invalid("1", "1", "1", "1", "1", "1", "1", "1");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "tckn | must be a valid Turkish identity number | geçerli bir T.C. kimlik numarası olmalıdır",
        "ykn | must be a valid foreign identity number | geçerli bir yabancı kimlik numarası olmalıdır",
        "vkn | must be a valid tax identification number | geçerli bir vergi kimlik numarası olmalıdır",
        "iban | must be a valid IBAN | geçerli bir IBAN olmalıdır",
        "plate | must be a valid license plate | geçerli bir araç plakası olmalıdır",
        "phone | must be a valid phone number | geçerli bir telefon numarası olmalıdır",
        "card | must be a valid card number | geçerli bir kart numarası olmalıdır",
        "postalCode | must be a valid postal code | geçerli bir posta kodu olmalıdır"
      })
  void messagesInBothLanguages(String property, String english, String turkish) {
    assertThat(message(Validators.english(), property)).isEqualTo(english);
    assertThat(message(Validators.turkish(), property)).isEqualTo(turkish);
  }

  @Test
  void englishIsUsedEvenWhenJvmDefaultLocaleIsTurkish() {
    Locale original = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      Validator validator = Validators.factory(Locale.ENGLISH).getValidator();

      assertThat(message(validator, "tckn")).isEqualTo("must be a valid Turkish identity number");
    } finally {
      Locale.setDefault(original);
    }
  }

  record Custom(@Tckn(message = "TCKN hatalı") String tckn) {}

  @Test
  void customMessageWins() {
    assertThat(Validators.messages(Validators.turkish().validate(new Custom("1"))))
        .containsExactly("TCKN hatalı");
  }

  @Test
  void bundlesDefineTheSameKeys() throws IOException {
    Properties english = load("ContributorValidationMessages.properties");
    Properties turkish = load("ContributorValidationMessages_tr.properties");
    Properties explicitEnglish = load("ContributorValidationMessages_en.properties");

    assertThat(english).hasSize(8);
    assertThat(turkish.keySet()).isEqualTo(english.keySet());
    assertThat(explicitEnglish).isEqualTo(english);
  }

  private static String message(Validator validator, String property) {
    return validator.validateProperty(Invalid.INSTANCE, property).iterator().next().getMessage();
  }

  private static Properties load(String name) throws IOException {
    Properties properties = new Properties();
    try (InputStream in = MessagesTest.class.getClassLoader().getResourceAsStream(name);
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }
}
