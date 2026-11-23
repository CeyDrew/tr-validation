package io.github.ceydrew.trvalidation.jakarta.internal;

import io.github.ceydrew.trvalidation.Ykn;
import io.github.ceydrew.trvalidation.jakarta.Tckn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link Tckn}. Internal; see the package documentation. */
public final class TcknValidator implements ConstraintValidator<Tckn, CharSequence> {

  private boolean allowYkn;

  /** Creates the validator; called by the Bean Validation provider. */
  public TcknValidator() {}

  @Override
  public void initialize(Tckn annotation) {
    allowYkn = annotation.allowYkn();
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    String s = value.toString();
    return io.github.ceydrew.trvalidation.Tckn.isValid(s) || (allowYkn && Ykn.isValid(s));
  }
}
