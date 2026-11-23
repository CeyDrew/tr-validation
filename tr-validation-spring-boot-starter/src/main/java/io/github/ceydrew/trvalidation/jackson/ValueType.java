package io.github.ceydrew.trvalidation.jackson;

import io.github.ceydrew.trvalidation.CardNumber;
import io.github.ceydrew.trvalidation.Iban;
import io.github.ceydrew.trvalidation.LicensePlate;
import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PostalCode;
import io.github.ceydrew.trvalidation.Tckn;
import io.github.ceydrew.trvalidation.Vkn;
import io.github.ceydrew.trvalidation.Ykn;
import java.util.List;
import java.util.function.Function;

/**
 * How a core value type is read from and written to a JSON string. Shared by the Jackson 2 and 3
 * modules.
 */
record ValueType<T>(
    Class<T> type, Function<String, T> of, Function<T, String> value, Function<T, String> masked) {

  static final List<ValueType<?>> ALL =
      List.of(
          new ValueType<>(Tckn.class, Tckn::of, Tckn::value, Tckn::masked),
          new ValueType<>(Ykn.class, Ykn::of, Ykn::value, Ykn::masked),
          new ValueType<>(Vkn.class, Vkn::of, Vkn::value, Vkn::masked),
          new ValueType<>(Iban.class, Iban::of, Iban::value, Iban::masked),
          new ValueType<>(CardNumber.class, CardNumber::of, CardNumber::value, CardNumber::masked),
          new ValueType<>(
              PhoneNumber.class, PhoneNumber::of, PhoneNumber::value, PhoneNumber::masked),
          new ValueType<>(
              LicensePlate.class, LicensePlate::of, LicensePlate::value, LicensePlate::masked),
          new ValueType<>(PostalCode.class, PostalCode::of, PostalCode::value, PostalCode::masked));

  /** The string written to JSON. */
  String write(Object instance, boolean masking) {
    T typed = type.cast(instance);
    return masking ? masked.apply(typed) : value.apply(typed);
  }

  /** Fails with a message naming the property if {@code @Masked} is not on a CharSequence. */
  static void checkMaskable(Class<?> rawType, String property) {
    if (!CharSequence.class.isAssignableFrom(rawType)) {
      throw new IllegalStateException(
          "@Masked is only supported on CharSequence properties, not on '"
              + property
              + "' of type "
              + rawType.getName());
    }
  }
}
