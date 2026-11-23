package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.PhoneNumber;
import io.github.ceydrew.trvalidation.PhoneType;
import io.github.ceydrew.trvalidation.jakarta.TrPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link TrPhone}. Internal; see the package documentation. */
public final class TrPhoneValidator implements ConstraintValidator<TrPhone, CharSequence> {

  private boolean mobileOnly;

  /** Creates the validator; called by the Bean Validation provider. */
  public TrPhoneValidator() {}

  @Override
  public void initialize(TrPhone annotation) {
    mobileOnly = annotation.mobileOnly();
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    String s = value.toString();
    return mobileOnly
        ? PhoneNumber.type(s).filter(type -> type == PhoneType.MOBILE).isPresent()
        : PhoneNumber.isValid(s);
  }
}
