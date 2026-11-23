package io.github.ceydrew.trvalidation.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class CustomerApiTest {

  private static final String VALID =
      """
      {
        "fullName": "Ayşe Yılmaz",
        "identityNumber": "100 000 001 46",
        "gsm": "0532 123 45 67",
        "iban": "tr33 0006 1005 1978 6457 8413 26",
        "plate": "34abc123",
        "postalCode": "34710"
      }
      """;

  private static final String INVALID =
      """
      {
        "fullName": "Ayşe Yılmaz",
        "identityNumber": "10000000147",
        "gsm": "0212 123 45 67",
        "iban": "DE89370400440532013000",
        "plate": "82 A 1234",
        "postalCode": "99999"
      }
      """;

  @Autowired WebApplicationContext context;

  MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void registersCustomerAndMasksPersonalDataInResponseAndLog(CapturedOutput output)
      throws Exception {
    mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/customers/")))
        .andExpect(jsonPath("$.fullName").value("Ayşe Yılmaz"))
        .andExpect(jsonPath("$.identityNumber").value("100******46"))
        .andExpect(jsonPath("$.gsm").value("+90 532 *** ** 67"))
        .andExpect(jsonPath("$.iban").value("TR33 **** **** **** **** **13 26"))
        .andExpect(jsonPath("$.plate").value("34 *** *23"))
        .andExpect(jsonPath("$.postalCode").value("34***"))
        .andExpect(content().string(not(containsString("10000000146"))))
        .andExpect(content().string(not(containsString("1234567"))));

    assertThat(output).contains("with GSM +90 532 *** ** 67").doesNotContain("5321234567");
  }

  @Test
  void acceptsForeignIdentityNumber() throws Exception {
    mvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("100 000 001 46", "99000000042")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.identityNumber").value("990******42"));
  }

  @Test
  void rejectsInvalidBodyInEnglish() throws Exception {
    mvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "en")
                .content(INVALID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.length()").value(5))
        .andExpect(jsonPath("$.errors[0].field").value("gsm"))
        .andExpect(jsonPath("$.errors[0].message").value("must be a valid phone number"))
        .andExpect(jsonPath("$.errors[1].field").value("iban"))
        .andExpect(jsonPath("$.errors[1].message").value("must be a valid IBAN"))
        .andExpect(jsonPath("$.errors[2].field").value("identityNumber"))
        .andExpect(jsonPath("$.errors[2].message").value("must be a valid Turkish identity number"))
        .andExpect(jsonPath("$.errors[3].field").value("plate"))
        .andExpect(jsonPath("$.errors[3].message").value("must be a valid license plate"))
        .andExpect(jsonPath("$.errors[4].field").value("postalCode"))
        .andExpect(jsonPath("$.errors[4].message").value("must be a valid postal code"))
        .andExpect(content().string(not(containsString("10000000147"))));
  }

  @Test
  void rejectsInvalidBodyInTurkish() throws Exception {
    mvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "tr-TR")
                .content(INVALID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("geçerli bir telefon numarası olmalıdır"))
        .andExpect(jsonPath("$.errors[1].message").value("geçerli bir IBAN olmalıdır"))
        .andExpect(
            jsonPath("$.errors[2].message").value("geçerli bir T.C. kimlik numarası olmalıdır"))
        .andExpect(jsonPath("$.errors[3].message").value("geçerli bir araç plakası olmalıdır"))
        .andExpect(jsonPath("$.errors[4].message").value("geçerli bir posta kodu olmalıdır"));
  }

  @Test
  void findsCustomerByValidatedRequestParameter() throws Exception {
    mvc.perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("100 000 001 46", "11111111110")))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/customers").param("identityNumber", "111-111-111-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.identityNumber").value("111******10"));
    mvc.perform(get("/api/customers").param("identityNumber", "12345678950"))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsInvalidRequestParameter() throws Exception {
    mvc.perform(
            get("/api/customers")
                .param("identityNumber", "10000000147")
                .header("Accept-Language", "tr"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("identityNumber"))
        .andExpect(
            jsonPath("$.errors[0].message").value("geçerli bir T.C. kimlik numarası olmalıdır"));
  }

  @Test
  void getById() throws Exception {
    String location =
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mvc.perform(get(location)).andExpect(status().isOk());
    mvc.perform(get("/api/customers/999999")).andExpect(status().isNotFound());
  }
}
