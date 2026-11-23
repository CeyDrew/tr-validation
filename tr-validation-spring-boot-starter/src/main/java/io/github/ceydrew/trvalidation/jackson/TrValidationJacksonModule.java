package io.github.ceydrew.trvalidation.jackson;

import io.github.ceydrew.trvalidation.InvalidValueException;
import java.util.List;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * Jackson 3 ({@code tools.jackson}) module for tr-validation, used by Spring Boot 4.
 *
 * <ul>
 *   <li>Properties annotated with {@link Masked} are written masked.
 *   <li>Core value types ({@code Tckn}, {@code Iban}, {@code PhoneNumber}, ...) are read from JSON
 *       strings via their {@code of} factory and written as their masked form, or their full value
 *       when masking is disabled. Invalid input fails with a message that does not contain it.
 * </ul>
 *
 * <pre>{@code
 * JsonMapper mapper = JsonMapper.builder().addModule(new TrValidationJacksonModule()).build();
 * mapper.writeValueAsString(Tckn.of("10000000146")); // "\"100******46\""
 * }</pre>
 *
 * <p>The Spring Boot starter registers this module automatically.
 */
public final class TrValidationJacksonModule extends SimpleModule {

  private static final long serialVersionUID = 1L;

  /** Creates the module with masking enabled. */
  public TrValidationJacksonModule() {
    this(true);
  }

  /**
   * Creates the module.
   *
   * <pre>{@code
   * new TrValidationJacksonModule(false); // value types written in full, @Masked ignored
   * }</pre>
   *
   * @param masking {@code true} to apply {@link Masked} and write value types masked
   */
  public TrValidationJacksonModule(boolean masking) {
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

  private static final class MaskingModifier extends ValueSerializerModifier {
    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(
        SerializationConfig config,
        BeanDescription.Supplier beanDesc,
        List<BeanPropertyWriter> properties) {
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

  private static final class MaskingSerializer extends ValueSerializer<Object> {
    private final MaskType maskType;

    MaskingSerializer(MaskType maskType) {
      this.maskType = maskType;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
      gen.writeString(maskType.apply(value.toString()));
    }
  }

  private static final class ValueTypeSerializer<T> extends ValueSerializer<T> {
    private final ValueType<T> valueType;
    private final boolean masking;

    ValueTypeSerializer(ValueType<T> valueType, boolean masking) {
      this.valueType = valueType;
      this.masking = masking;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
      gen.writeString(valueType.write(value, masking));
    }
  }

  private static final class ValueTypeDeserializer<T> extends ValueDeserializer<T> {
    private final ValueType<T> valueType;

    ValueTypeDeserializer(ValueType<T> valueType) {
      this.valueType = valueType;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) {
      if (p.currentToken() != JsonToken.VALUE_STRING) {
        return ctxt.reportInputMismatch(
            this, "Expected a JSON string for %s", valueType.type().getSimpleName());
      }
      try {
        return valueType.of().apply(p.getString());
      } catch (InvalidValueException e) {
        // the message names the type and reason only, never the rejected value
        return ctxt.reportInputMismatch(this, "%s", e.getMessage());
      }
    }
  }
}
