package io.github.ceydrew.trvalidation.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;

class TrValidationJackson2ModuleTest extends JacksonModuleContract {

  private static final ObjectMapper MASKING =
      new ObjectMapper().registerModule(new TrValidationJackson2Module());
  private static final ObjectMapper PLAIN =
      new ObjectMapper().registerModule(new TrValidationJackson2Module(false));

  @Override
  String write(Object value, boolean masking) {
    try {
      return (masking ? MASKING : PLAIN).writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  <T> T read(String json, Class<T> type) {
    try {
      return MASKING.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
