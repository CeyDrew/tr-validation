package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.jakarta.Ykn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link Ykn}. Internal; see the package documentation. */
public final class YknValidator implements ConstraintValidator<Ykn, CharSequence> {

  /** Creates the validator; called by the Bean Validation provider. */
  public YknValidator() {}

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || io.github.ceydrew.trvalidation.Ykn.isValid(value.toString());
  }
}
