package io.github.ceydrew.trvalidation.jackson;

import tools.jackson.databind.json.JsonMapper;

class TrValidationJacksonModuleTest extends JacksonModuleContract {

  private static final JsonMapper MASKING =
      JsonMapper.builder().addModule(new TrValidationJacksonModule()).build();
  private static final JsonMapper PLAIN =
      JsonMapper.builder().addModule(new TrValidationJacksonModule(false)).build();

  @Override
  String write(Object value, boolean masking) {
    return (masking ? MASKING : PLAIN).writeValueAsString(value);
  }

  @Override
  <T> T read(String json, Class<T> type) {
    return MASKING.readValue(json, type);
  }
}
