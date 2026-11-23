package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.jakarta.TrPlate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link TrPlate}. Internal; see the package documentation. */
public final class TrPlateValidator implements ConstraintValidator<TrPlate, CharSequence> {

  /** Creates the validator; called by the Bean Validation provider. */
  public TrPlateValidator() {}

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || io.github.ceydrew.trvalidation.LicensePlate.isValid(value.toString());
  }
}
