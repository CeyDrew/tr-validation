package io.github.ceydrew.trvalidation.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.github.ceydrew.trvalidation.InvalidValueException;
import java.io.IOException;
import java.util.List;

/**
 * Jackson 2 ({@code com.fasterxml.jackson}) module for tr-validation, used by Spring Boot 3. Same
 * behavior as {@link TrValidationJacksonModule}.
 *
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper().registerModule(new TrValidationJackson2Module());
 * mapper.writeValueAsString(Tckn.of("10000000146")); // "\"100******46\""
 * }</pre>
 *
 * <p>The Spring Boot starter registers this module automatically when Jackson 2 is present.
 */
public final class TrValidationJackson2Module extends SimpleModule {

  private static final long serialVersionUID = 1L;

  /** Creates the module with masking enabled. */
  public TrValidationJackson2Module() {
    this(true);
  }

  /**
   * Creates the module.
   *
   * <pre>{@code
   * new TrValidationJackson2Module(false); // value types written in full, @Masked ignored
   * }</pre>
   *
   * @param masking {@code true} to apply {@link Masked} and write value types masked
   */
  public TrValidationJackson2Module(boolean masking) {
    super("tr-validation");
    for (ValueType<?> valueType : ValueType.ALL) {
      register(valueType, masking);
    }
    if (masking) {
      setSerializerModifier(new MaskingModifier());
    }
  }

  private <T> void register(ValueType<T> valueType, boolean masking) {
    addSerializer(valueType.type(), new ValueTypeSerializer<>(valueType, masking));
    addDeserializer(valueType.type(), new ValueTypeDeserializer<>(valueType));
  }

  private static final class MaskingModifier extends BeanSerializerModifier {
    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(
        SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> properties) {
      for (BeanPropertyWriter writer : properties) {
        Masked masked = writer.getAnnotation(Masked.class);
        if (masked != null) {
          ValueType.checkMaskable(writer.getType().getRawClass(), writer.getName());
          writer.assignSerializer(new MaskingSerializer(masked.value()));
        }
      }
      return properties;
    }
  }

  private static final class MaskingSerializer extends JsonSerializer<Object> {
    private final MaskType maskType;

    MaskingSerializer(MaskType maskType) {
      this.maskType = maskType;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeString(maskType.apply(value.toString()));
    }
  }

  private static final class ValueTypeSerializer<T> extends JsonSerializer<T> {
    private final ValueType<T> valueType;
    private final boolean masking;

    ValueTypeSerializer(ValueType<T> valueType, boolean masking) {
      this.valueType = valueType;
      this.masking = masking;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeString(valueType.write(value, masking));
    }
  }

  private static final class ValueTypeDeserializer<T> extends JsonDeserializer<T> {
    private final ValueType<T> valueType;

    ValueTypeDeserializer(ValueType<T> valueType) {
      this.valueType = valueType;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.currentToken() != JsonToken.VALUE_STRING) {
        return ctxt.reportInputMismatch(
            this, "Expected a JSON string for %s", valueType.type().getSimpleName());
      }
      try {
        return valueType.of().apply(p.getText());
      } catch (InvalidValueException e) {
        // the message names the type and reason only, never the rejected value
        return ctxt.reportInputMismatch(this, "%s", e.getMessage());
      }
    }
  }
}
