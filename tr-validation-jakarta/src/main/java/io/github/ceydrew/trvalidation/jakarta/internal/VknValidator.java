package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.jakarta.Vkn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link Vkn}. Internal; see the package documentation. */
public final class VknValidator implements ConstraintValidator<Vkn, CharSequence> {

  /** Creates the validator; called by the Bean Validation provider. */
  public VknValidator() {}

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || io.github.ceydrew.trvalidation.Vkn.isValid(value.toString());
  }
}
