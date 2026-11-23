package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.jakarta.TrPostalCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link TrPostalCode}. Internal; see the package documentation. */
public final class TrPostalCodeValidator
    implements ConstraintValidator<TrPostalCode, CharSequence> {

  /** Creates the validator; called by the Bean Validation provider. */
  public TrPostalCodeValidator() {}

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || io.github.ceydrew.trvalidation.PostalCode.isValid(value.toString());
  }
}
