package io.github.ceydrew.trvalidation.boot3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.jackson.MaskType;
import io.github.ceydrew.trvalidation.jackson.Masked;
import io.github.ceydrew.trvalidation.jackson.TrValidationJackson2Module;
import io.github.ceydrew.trvalidation.jakarta.Tckn;
import io.github.ceydrew.trvalidation.jakarta.TrPhone;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The starter works unchanged in a Spring Boot 3 / Jackson 2 application. */
@SpringBootTest(classes = SpringBoot3CompatibilityTest.App.class)
@AutoConfigureMockMvc
class SpringBoot3CompatibilityTest {

  @SpringBootApplication
  @org.springframework.context.annotation.Import(Controller.class)
  static class App {}

  record Request(@Tckn String tckn, @TrPhone(mobileOnly = true) String gsm) {}

  record Response(@Masked(MaskType.TCKN) String tckn, PhoneNumber gsm) {}

  @RestController
  static class Controller {
    @PostMapping("/echo")
    Response echo(@Valid @RequestBody Request request) {
      return new Response(request.tckn(), PhoneNumber.of(request.gsm()));
    }

    @GetMapping("/check")
    String check(@RequestParam @Tckn String tckn) {
      return "ok";
    }
  }

  @Autowired ApplicationContext context;
  @Autowired MockMvc mvc;

  @Test
  void runsOnSpringBoot3WithJackson2() {
    assertThat(SpringBootVersion.getVersion()).startsWith("3.");
    assertThat(context.getBeansOfType(TrValidationJackson2Module.class)).hasSize(1);
    assertThat(context.containsBean("trValidationJacksonModule")).isFalse();
  }

  @Test
  void masksResponses() throws Exception {
    mvc.perform(
            post("/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tckn\":\"10000000146\",\"gsm\":\"0532 123 45 67\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tckn").value("100******46"))
        .andExpect(jsonPath("$.gsm").value("+90 532 *** ** 67"));
  }

  @Test
  void validatesBodyWithTurkishMessages() throws Exception {
    mvc.perform(
            post("/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "tr")
                .content("{\"tckn\":\"10000000147\",\"gsm\":\"0212 123 45 67\"}"))
        .andExpect(status().isBadRequest());
  }

  @Autowired jakarta.validation.Validator validator;

  @Test
  void bootValidatorResolvesMessagesForTheRequestLocale() {
    Request invalid = new Request("10000000147", null);
    try {
      org.springframework.context.i18n.LocaleContextHolder.setLocale(
          java.util.Locale.forLanguageTag("tr-TR"));
      assertThat(validator.validate(invalid).iterator().next().getMessage())
          .isEqualTo("geçerli bir T.C. kimlik numarası olmalıdır");
      org.springframework.context.i18n.LocaleContextHolder.setLocale(java.util.Locale.ENGLISH);
      assertThat(validator.validate(invalid).iterator().next().getMessage())
          .isEqualTo("must be a valid Turkish identity number");
    } finally {
      org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  void validatesRequestParameters() throws Exception {
    mvc.perform(get("/check").param("tckn", "10000000146")).andExpect(status().isOk());
    mvc.perform(get("/check").param("tckn", "10000000147")).andExpect(status().isBadRequest());
  }
}
